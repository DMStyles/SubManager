const { createClient } = require('@supabase/supabase-js');
const admin = require('firebase-admin');

// 1. Initialize Supabase
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_KEY; // Service Role Key or Anon Key

if (!supabaseUrl || !supabaseKey) {
  console.error("Missing Supabase credentials in environment.");
  process.exit(1);
}
const supabase = createClient(supabaseUrl, supabaseKey);

// 2. Initialize Firebase Admin
let firebaseConfigStr = process.env.FIREBASE_SERVICE_ACCOUNT;
if (!firebaseConfigStr) {
  console.error("Missing FIREBASE_SERVICE_ACCOUNT in environment.");
  process.exit(1);
}
const serviceAccount = JSON.parse(firebaseConfigStr);

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

function getDynamicMonthsUsed(startDateStr) {
  const start = new Date(startDateStr);
  const now = new Date();
  const used = (now.getFullYear() - start.getFullYear()) * 12 + (now.getMonth() - start.getMonth()) + 1;
  return Math.max(1, used);
}

// Calculate days until billing day
function getDaysUntilBilling(billingDay) {
  const now = new Date();
  const currentDay = now.getDate();
  if (currentDay <= billingDay) {
    return billingDay - currentDay;
  } else {
    const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
    return (daysInMonth - currentDay) + billingDay;
  }
}

async function runReminders() {
  const now = new Date();
  const currentDay = now.getDate();

  console.log(`Running reminder job for day ${currentDay}...`);

  // Fetch all subscriptions
  const { data: subscriptions, error: subErr } = await supabase.from('subscriptions').select('*');
  if (subErr) {
    console.error("Failed to fetch subscriptions:", subErr);
    process.exit(1);
  }

  // Fetch all members to get FCM tokens
  const { data: members, error: memErr } = await supabase.from('members').select('*');
  if (memErr) {
    console.error("Failed to fetch members:", memErr);
    process.exit(1);
  }

  const memberMap = new Map();
  members.forEach(m => memberMap.set(m.id, m));

  // Fetch all payments
  const { data: payments, error: payErr } = await supabase.from('payments').select('*');
  if (payErr) {
    console.error("Failed to fetch payments:", payErr);
    process.exit(1);
  }

  // Fetch all subscription_members
  const { data: subMembers, error: smErr } = await supabase.from('subscription_members').select('*');
  if (smErr) {
    console.error("Failed to fetch subscription_members:", smErr);
    process.exit(1);
  }

  for (const sub of subscriptions) {
    const billingDay = sub.billing_day;
    const daysUntilBilling = getDaysUntilBilling(billingDay);
    const monthsUsed = getDynamicMonthsUsed(sub.start_date);

    // Only process if it's the exact billing day or 3 days before or 3 days after
    const isDueToday = (currentDay === billingDay);
    const isUpcoming = (daysUntilBilling > 0 && daysUntilBilling <= 3);
    const isOverdue = (currentDay > billingDay && currentDay <= billingDay + 3) || (billingDay - currentDay < -25); // simple wrap logic

    if (!isDueToday && !isUpcoming && !isOverdue) {
        console.log(`[${sub.name}] Billing day is ${billingDay}. Today is ${currentDay}. Skipping.`);
        continue;
    }

    console.log(`[${sub.name}] Checking members... (Billing Day: ${billingDay}, Cost: ${sub.monthly_cost})`);

    const groupMembers = subMembers.filter(sm => sm.subscription_id === sub.id);

    for (const sm of groupMembers) {
      const member = memberMap.get(sm.member_id);
      if (!member) continue;

      const memberPayments = payments.filter(p => p.subscription_id === sub.id && p.member_id === member.id);
      const totalPaid = memberPayments.reduce((acc, p) => acc + p.amount, 0);
      const totalMonthsPaid = Math.floor(totalPaid / sub.monthly_cost);
      const remainingBalance = totalMonthsPaid - monthsUsed;

      if (remainingBalance >= 0) {
        console.log(` - ${member.name} is paid up (Balance: +${remainingBalance}).`);
        continue;
      }

      let messageTitle = "";
      let messageBody = "";

      if (isDueToday) {
        messageTitle = `${sub.name} Payment Due Today! ⏰`;
        messageBody = `Heads up! Your payment of Rs. ${sub.monthly_cost} for ${sub.name} is due TODAY.`;
      } else if (isUpcoming) {
        messageTitle = `Upcoming: ${sub.name} Payment Due in ${daysUntilBilling} days`;
        messageBody = `Reminder: Your payment of Rs. ${sub.monthly_cost} for ${sub.name} is due on the ${billingDay}th!`;
      } else if (isOverdue) {
        messageTitle = `${sub.name} Payment OVERDUE! ⚠️`;
        messageBody = `Your payment for ${sub.name} is OVERDUE! Please pay your outstanding balance ASAP.`;
      }

      console.log(` - ${member.name} is due (Balance: ${remainingBalance}). Sending: ${messageTitle}`);

      // Add to in-app notifications
      await supabase.from('notifications').insert({
        member_id: member.id,
        subscription_id: sub.id,
        message: messageBody,
        is_read: false
      });
      console.log(`   > Created in-app notification for ${member.name}`);

      // Send Push Notification if FCM token exists
      if (member.fcm_token) {
        const payload = {
          notification: {
            title: messageTitle,
            body: messageBody
          },
          token: member.fcm_token
        };

        try {
          const response = await admin.messaging().send(payload);
          console.log(`   > Sent push notification to ${member.name}: ${response}`);
        } catch (error) {
          console.error(`   > Error sending push to ${member.name}:`, error);
        }
      } else {
        console.log(`   > No FCM token for ${member.name}, skipping push.`);
      }
    }
  }

  console.log("Finished processing reminders.");
}

runReminders();
