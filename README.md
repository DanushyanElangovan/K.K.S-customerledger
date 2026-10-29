# Customer Ledger (native Android, Java)

Customer accounts, purchases, payments and cheques with automatic balances, and a daily
phone notification for cheques due within 2 days.

## Run it
1. File > Open in Android Studio and choose this folder (the one containing `settings.gradle.kts`).
2. Wait for Gradle sync (internet needed the first time). Set Settings > Build Tools > Gradle >
   Gradle JDK to jbr-17 if sync complains.
3. Turn on USB debugging on the phone, plug it in, pick it in the device list, press Run.
   For a file to share: Build > Build Bundle(s) / APK(s) > Build APK(s).

## Where things live
All code is under `app/src/main/java/com/ledger/customerledger/`

| Folder | What it holds | Change it when you want to... |
|---|---|---|
| `AppConfig.java` | currency text, reminder days, reminder hour, country code | change Rs., 2 days, 9 AM, 94 |
| `model/` | plain data classes (Customer, Purchase, Payment, Cheque...) | add a field to a record |
| `data/LedgerDb.java` | table definitions | add a database column |
| `data/LedgerRepository.java` | every read and write, and the balance rule | change a calculation or a list |
| `util/` | money, dates, phone, reminder wording, CSV, file export | change formats or the message text |
| `reminder/` | daily job and the notification | change when/how the notification appears |
| `ui/main/` | MainActivity plus the Home, Customers, Cheques tabs | change a tab's behaviour |
| `ui/customer/` | the customer page | change the customer page |
| `ui/dialog/` | the forms (customer, purchase, payment, cheque, reminder) | change a form |
| `ui/adapter/` | how list rows are filled in | change what a row shows |
| `ui/cheque/ChequeActions.java` | what the cheque buttons do | change a cheque button |
| `res/layout/` | screen and form designs (XML) | change how a screen looks |
| `res/values/` | colours, text styles, theme | change colours or fonts |

## Recipes
**Change the reminder wording:** edit `util/ReminderText.java`.

**Remind 3 days before instead of 2:** set `REMIND_DAYS_BEFORE = 3` in `AppConfig.java`
(also change the words "2 days" in `HomeFragment.java` and `NotificationHelper.java`).

**Add a field to customers (example: address):**
1. `model/Customer.java`: add `public String address = "";`
2. `data/LedgerDb.java`: raise `VERSION` to 2 and add in `onUpgrade`:
   `if (oldVersion < 2) db.execSQL("ALTER TABLE customers ADD COLUMN address TEXT");`
   Also add `address TEXT` in the CREATE TABLE for new installs.
3. `data/LedgerRepository.java`: read it in `queryAccounts` (add `c.address` to the SELECT and set it),
   and write it in `saveCustomer`.
4. `res/layout/dialog_customer.xml`: copy the Notes field, rename its id to `input_address`.
5. `ui/dialog/CustomerDialog.java`: set and read `b.inputAddress` like `inputNote`.

**Change the balance rule:** edit `CustomerAccount.getBalance()` in `model/CustomerAccount.java`
and the SUM queries at the top of `LedgerRepository.java`.

## Notes
- Data is stored in a SQLite database on the phone. Android's automatic backup includes it
  when the phone is signed in to a Google account. "Export" buttons save CSV files to Downloads.
- Notification permission is requested on first launch (Android 13+).
