rem call gradlew assembleRelease
copy C:\Users\Marcello\AndroidStudioProjects\Inevents\internationsevents\build\apk\internationsevents-release-unsigned.apk C:\Users\Marcello\AndroidStudioProjects\Inevents\internationsevents\build\apk\internationsevents.apk 
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore C:\Users\Marcello\Dropbox\ks\androidks C:\Users\Marcello\AndroidStudioProjects\Inevents\internationsevents\build\apk\internationsevents.apk marcellourbani
del C:\Users\Marcello\AndroidStudioProjects\Inevents\internationsevents\build\apk\InEvents.apk
"C:\Program Files (x86)\Android\android-studio\sdk\tools\zipalign.exe" -v 4 C:\Users\Marcello\AndroidStudioProjects\Inevents\internationsevents\build\apk\internationsevents.apk C:\Users\Marcello\AndroidStudioProjects\Inevents\internationsevents\build\apk\InEvents.apk 
