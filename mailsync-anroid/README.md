mailSync-android
---
**Version: 0.3**

Version Features
--
v0.1
--
1. Log in
2. Display emails when downloading (no need to wait for all emails to be completely downloaded);
3. Display emails when syncing to laptop (no need to wait for all emails to be completely synced); 
4. Can display and sync new emails;
5. "Automatically" get NDN unicast route.

v0.2
--
1. Improve the performance of appending message into GreenMail storage (still has bottleneck due to 
GreenMail inner implementation)
2. Automatically detect Internet availability and switch between NDN mode and Internet mode

v0.3
--
1. Relay traffic
2. Store emails into NDN storage according to the Gmail APP behavior (If Gmail wants to fetch 10 latest email, we only store these 10 emails into NDN storage)
3. Sync between laptop and Android phone without overwriting the old emails on the laptop.

Future Work
---
v0.1
--
1. Performance;
2. Emails displaying order;
3. Automatically change between NDN mode and Internet mode (by detecting if the Internet is available);
4. Let Gmail client action to trigger the mailSync-android downloading process (not by clicking the 
Fetch button in mailSync-android);

v0.2
--
1. Emails displaying order;
2. Let Gmail client action to trigger the mailSync-android downloading process (not by clicking the 
   Fetch button in mailSync-android);
  
v0.3
--
1. Switch between NDN mode and Internet mode smoothly. (There are still problems with the automatic detection of the Internet availability and switch between NDN mode and Internet mode. But I think it should not be difficult to solve.)
2. Reproduce the problem related to the NFD-Android and solve it. 
3. Refine the user experience
...



