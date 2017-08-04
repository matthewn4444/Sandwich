# Subwich

This is an Android app to move subtitles to a usb stick with an interface for the Oppo udp-203 device.

The Oppo udp-203 device is a 4k blu-ray player that allows you to externally add subtitles from a usb stick. The subtitles are in SRT format placed in the folder path from the root of the usb **/sub/sub.srt**.

This app organizes your subtitle files using a special format on your phone, then you do not need to go to a PC to swap subtitles with. When you want to load different subtitles to your usb, attach the usb to the phone's usb port, select the video series, and then the individual episode to send to device. It will move the subtitle for you. Then you can plug the usb into the Oppo device and start watching with subtitles.

## Format
The format to put your subtitles into your phone is a folder name and inside you can store a cover image (cover.png/jpg) and all the subtitles with the file name as the episode number. For example, 


    ../subtitles/
        |-- My Video Series/
            |-- cover.jpg
            |-- 1.srt
            |-- 2.srt
            |-- 3.srt
            |-- 10.srt
        |-- Another Video Series
            ...

You do not need all the episodes but whatever is listed will be found. The file name must have a number to show its episode number (which is displayed in the app). The cover is used for interface purposes. Any folders that contain no subtitles will be ignore.

## How to Use
When you open the app, please allow "write permissions" via dialog. Then you will need to select the root for all your subtitles. Then the app will read the folder contents for any subtitles and display the name of the folder with the cover image (if there is any). 

Tap the video series you want to copy subtitles unto usb. It will show all the episodes listed in the folder. Please insert usb to device and make sure it is active. Tap the episode you want to copy and hit ok (that you want to copy the subtitle over).

Only for the first time, you will be prompted to select the root of your usb to allow this app to have access to write to it. Once you do this the app will copy the subtitle to usb. Please eject the usb from device and insert into Oppo BD player.


