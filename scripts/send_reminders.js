const { createClient } = require('@supabase/supabase-js');
const admin = require('firebase-admin');

// 1. Initialize Supabase
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_KEY; // Service Role Key

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

// Spotify Plan Constants
const MONTHLY_COST = 211.0;
const BILLING_DAY = 8;
const START_YEAR = 2026;
const START_MONTH = 5; // May 2026

function getDynamicMonthsUsed() {
  const now = new Date();
  const y = now.getFullYear();
  const m = now.getMonth() + 1;
  const used = (y - START_YEAR) * 12 + (m - START_MONTH) + 1;
  return Math.max(1, used);
}

async function runReminders() {
  const dynamicMonthsUsed = getDynamicMonthsUsed();
  const now = new Date();
  const currentDay = now.getDate();
  const forceSend = process.env.FORCE_SEND === 'true' || process.env.MANUAL_RUN === 'true';

  console.log(`=== Spotify Daily Reminder Job ===`);
  console.log(`Current Date: ${now.toISOString().split('T')[0]} (Day ${currentDay})`);
  console.log(`Months Used from Start: ${dynamicMonthsUsed}`);
  console.log(`Force Send Override: ${forceSend}`);

  // Schedule days:
  // - Days 5, 6, 7: Upcoming (3, 2, 1 days before the 8th)
  // - Day 8: Due TODAY
  // - Days 9, 12, 15, 18, 21, 24, 27: Overdue reminders
  const scheduledDays = [5, 6, 7, 8, 9, 12, 15, 18, 21, 24, 27];
  const isScheduledDay = scheduledDays.includes(currentDay);

  if (!isScheduledDay && !forceSend) {
    console.log(`Today (Day ${currentDay}) is not a scheduled reminder day. Skipping.`);
    console.log(`Scheduled reminder days: ${scheduledDays.join(', ')}`);
    console.log(`Finished processing reminders.`);
    return;
  }

  // 1. Fetch all members
  const { data: members, error: memErr } = await supabase.from('members').select('*');
  if (memErr) {
    console.error("Failed to fetch members:", memErr);
    process.exit(1);
  }

  // 2. Fetch all payments
  const { data: payments, error: payErr } = await supabase.from('payments').select('*');
  if (payErr) {
    console.error("Failed to fetch payments:", payErr);
    process.exit(1);
  }

  console.log(`Checking ${members.length} members...`);

  for (const member of members) {
    const memberPayments = payments.filter(p => p.member_id === member.id);
    const totalPaid = memberPayments.reduce((acc, p) => acc + p.amount, 0);
    const totalMonthsPaid = Math.floor(totalPaid / MONTHLY_COST);
    const remainingBalance = totalMonthsPaid - dynamicMonthsUsed;

    if (remainingBalance >= 0) {
      console.log(`✓ ${member.name}: Paid up (Balance: +${remainingBalance}).`);
      continue;
    }

    let messageTitle = "";
    let messageBody = "";

    if (remainingBalance === -1 && currentDay <= BILLING_DAY) {
      const daysLeft = BILLING_DAY - currentDay;
      if (daysLeft === 0) {
        messageTitle = "Spotify Payment Due Today! ⏰";
        messageBody = `Heads up! Your Spotify payment of Rs. ${MONTHLY_COST} is due TODAY.`;
      } else {
        messageTitle = `Upcoming: Spotify Payment Due in ${daysLeft} day${daysLeft > 1 ? 's' : ''}`;
        messageBody = `Reminder: Your Spotify payment of Rs. ${MONTHLY_COST} is due on the 8th!`;
      }
    } else {
      const monthsOwed = Math.abs(remainingBalance);
      messageTitle = "Spotify Payment OVERDUE! ⚠️";
      messageBody = `Your Spotify payment is OVERDUE (${monthsOwed} month${monthsOwed > 1 ? 's' : ''})! Please pay your outstanding balance of Rs. ${monthsOwed * MONTHLY_COST} ASAP.`;
    }

    console.log(`! ${member.name}: Due (Balance: ${remainingBalance}). Notification: "${messageTitle}"`);

    // In-app notification
    try {
      await supabase.from('notifications').insert({
        member_id: member.id,
        message: messageBody,
        is_read: false
      });
      console.log(`   > Created in-app notification for ${member.name}`);
    } catch (e) {
      console.error(`   > Failed to create in-app notification:`, e.message);
    }

    // Push notification via FCM
    if (member.fcm_token) {
      const payload = {
        notification: {
          title: messageTitle,
          body: messageBody
        },
        data: {
          title: messageTitle,
          body: messageBody,
          channelId: 'payment_reminders'
        },
        android: {
          priority: 'high',
          notification: {
            channelId: 'payment_reminders',
            sound: 'default',
            priority: 'high'
          }
        },
        token: member.fcm_token
      };

      try {
        const response = await admin.messaging().send(payload);
        console.log(`   > Sent push notification to ${member.name}: ${response}`);
      } catch (error) {
        console.error(`   > Error sending push to ${member.name}:`, error.message);
        if (error.code === 'messaging/registration-token-not-registered' || error.code === 'messaging/invalid-registration-token') {
          console.log(`   > Token no longer valid. Clearing stale token for ${member.name}.`);
          await supabase.from('members').update({ fcm_token: null }).eq('id', member.id);
        }
      }
    } else {
      console.log(`   > No FCM token registered for ${member.name}, push skipped.`);
    }
  }

  console.log("Finished processing reminders.");
}

runReminders();
