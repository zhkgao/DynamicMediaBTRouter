# Dynamic Media BT Router
A simple background service which detects any playback on the global audiomix and then opens the BluetoothSco channel so the audio is played on your mono Bluetooth device (hands free profile).

### Idea
Enable the _dynamic_ playback of any media sound on the Bluetooth device. Especially for the direction guide of your navigation software.
The Bluetooth channel should be opened when a playback is started and closed after the playback is finished.

### Why another app? There a several that do the same!
No, not exactly! All other apps I've seen are not dynamic or (I suppose) use the isMusicActive() function which in fact isn't reliable.
Therefore a use the Visualizer API to grab anonymized info from the global audiomix and can for sure determine if audio is played.
The visualizer API function I use is only available since API Level 19 (Kitkat - Android 4.4). For API Levels below I use the isMusicActive() function as well...

### State based redirection
The service is only startable if the bluetooth adapter is turned on, everything else wouldn't make sense would it?
The redirection only starts if a bluetooth device is connected to the hands free bluetooth profile. If the hands free connection isn't available anymore the redirection stops.

**Static redirection**

If you choose this option the audio redirection will start as soon as you start the service and only stops with the service. No audio information is analyzed.

If you choose the reconnect after call option the service restarts itself after a voice call is terminated (the telephone app has priority over the sco channel).
The service also restarts if you end the "call" for media redirection. This option is only available in combination with static redirection.

### Automatic Start/Stop

There is one option which stops the service if:
- the connected device is no longer used for the hands-free profile
- the connected device disconnects
- bluetooth is turned off

There is one option which starts the service if:
- a device connects with the hands-free profile

### Intent to start/stop the service
You could use Tasker or something else to send a intent to start the service.
Keep in mind that the intents override the checks for a valid connection!
If you start the service with no device connected the audio route is not predictable.

Send of the following intents:
- net.philipp_koch.dynamicmediabtrouter.ON
- net.philipp_koch.dynamicmediabtrouter.OFF

### Requirements
- An Android device with the minimum API Level 11 (Honeycomb - Android 3.0)
    - API Level 18 (Jelly Bean - Android 4.3) for a raw bluetooth stream. Lower version may require you to accept the "call"
    - API Level 19 (Kitkit - Android 4.4) for audio recognition via the visualizer API.
- A bluetooth device which supports the hands free profile (usually headsets or car speakerphones)
- No task managers! They're crap and nothing else! They kill the background service so don't complain!
- _optional_: Xposed framework 54 - To hook the audio playback function of several navigation apps and add a delay resulting that the service has enough time to open the channel so no information is lost
    - **currently supported apps:**
    - _none. work in progress_

#### Working as designed (dynamic mode)
If you end the "call" the redirection doesn't stop and audio is routed to nowhere.
Because the app was intended for navigation apps this is a useful design:

If redirection starts and you are already aware of the information presented you could end the "call" as soon it starts.
After the information is played the service will close the channel as usual and open it again as soon as audio is detected.

If you use the app to redirect music for an ongoing time and you end the "call" accidentally you have to pause the music for at least a few seconds. The service will stop the redirection and start it again as you press play.

##### "_The sound is shitty fix that!!1!_"
I can't! The hands free profile isn't intended for high quality. Use A2DP instead!

## What’s left to do?
Some things
- Preferences for static redirecting a sometimes lost
- (xposed) Adding a delay/pause while opening the bluetooth channel -- currently audio is lost
    - in general without xposed
