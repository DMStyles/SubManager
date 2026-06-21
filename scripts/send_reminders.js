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

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// Calculate how many months have passed since May 2026
function getDynamicMonthsUsed() {
  const START_YEAR = 2026;
  const START_MONTH = 5; // May
  const now = new Date();
  const y = now.getFullYear();
  const m = now.getMonth() + 1;
  const used = (y - START_YEAR) * 12 + (m - START_MONTH) + 1;
  return Math.max(1, used);
}

// Calculate days until the 8th of the month
function getDaysUntilPayment() {
  const now = new Date();
  const day = now.getDate();
  if (day <= 8) {
    return 8 - day;
  } else {
    const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
    return (daysInMonth - day) + 8;
  }
}

async function runReminders() {
  const dynamicMonthsUsed = getDynamicMonthsUsed();
  const daysUntilPayment = getDaysUntilPayment();
  const MONTHLY_COST = 211.0;

  // We only want to spam push notifications if we are close to the 8th!
  // e.g. 1 to 4 days before, or on the day of (0 days), or if it's way overdue but maybe only send once a week?
  // Let's just send it if daysUntilPayment <= 4
  if (daysUntilPayment > 4 && daysUntilPayment < 25) {
    console.log(`It's not close to the 8th (Days until: ${daysUntilPayment}). Skipping notifications.`);
    return;
  }

  // Fetch all members
  const { data: members, error: membersErr } = await supabase.from('members').select('*');
  if (membersErr) {
    console.error("Failed to fetch members:", membersErr);
    process.exit(1);
  }

  // Fetch all payments
  const { data: payments, error: paymentsErr } = await supabase.from('payments').select('*');
  if (paymentsErr) {
    console.error("Failed to fetch payments:", paymentsErr);
    process.exit(1);
  }

  for (const member of members) {
    const memberPayments = payments.filter(p => p.member_id === member.id);
    const totalPaid = memberPayments.reduce((acc, p) => acc + p.amount, 0);
    const totalMonthsPaid = Math.floor(totalPaid / MONTHLY_COST);
    const remainingBalance = totalMonthsPaid - dynamicMonthsUsed;

    if (remainingBalance <= 0) {
      console.log(`Member ${member.name} is due (Balance: ${remainingBalance}).`);

      let messageTitle = "Spotify Payment Due";
      let messageBody = `Reminder: Your Spotify payment of Rs. 212 is due on the 8th!`;

      if (daysUntilPayment === 0) {
        messageBody = `Heads up! Your Spotify payment is due TODAY.`;
      } else if (remainingBalance < 0) {
        messageBody = `Your Spotify payment is OVERDUE! Please pay Rs. 212 ASAP.`;
      }

      // Add to in-app notifications
      await supabase.from('notifications').insert({
        member_id: member.id,
        message: messageBody,
        is_read: false
      });
      console.log(`- Created in-app notification for ${member.name}`);

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
          console.log(`- Sent push notification to ${member.name}: ${response}`);
        } catch (error) {
          console.error(`- Error sending push to ${member.name}:`, error);
        }
      } else {
        console.log(`- No FCM token for ${member.name}, skipping push.`);
      }
    } else {
      console.log(`Member ${member.name} is paid up (Balance: +${remainingBalance}).`);
    }
  }

  console.log("Finished processing reminders.");
}

runReminders();
