Wi-Fi Mapper
=================

A personal Android application for mapping the access point strengths for a current location. 
The results will be used to create a fingerprint of an indoor position.

![Mapping][1] 

Register mapping scans for the current position. Set a unique session id, number of scans to be done, and delay (in seconds) between each scan.

![Registering scans][4] 

For each scan it will register the phone's orientation (x, y, z) and all visible access points. 

![Example Image][2]

List of all visible access points, sorted by signal strength.

![Example Image][3]

Tag access points.

_____

Scans are stored in 'sdcard/wifimapper/mapper/[id]_[session]_[date].txt'. 
SSID, MAC address, signal, and frequency are stored for each access point.


	#2|1|1344938963|5|5|00:1d:70:98:b9:81|HTC Vision, 4.0.4, 0.85.0005
	$1|1344938964
	?[155.0, -56.0, 1.0]
	%SSID|00:1d:70:98:b9:81|-66|2412
	%SSID|00:21:d8:44:18:71|-75|2437
	$2|1344938970
	?[155.0, -56.0, 1.0]
	%SSID|00:1d:70:98:b9:81|-66|2412
	%SSID|00:21:d8:44:18:71|-69|2437
	...
	
Access points tags are stored in 'sdcard/wifimapper/tagger.txt'.
	
	00\:1d\:a1\:76\:d6\:51=TAG
	00\:1d\:70\:98\:b9\:81=TAG oppe
	00\:21\:d8\:44\:2e\:71=TAG
	...

	
*This is an old application and has a lot of possible improvements*

 [1]: https://lh3.googleusercontent.com/-wmAuCO7opOA/UbCKnqSYChI/AAAAAAAACSg/SOvvZhbdw3w/s400/Screenshot_2013-06-06-15-07-01.png
 [2]: https://lh5.googleusercontent.com/-EI6Bhu_DjiY/UbCKmG2swYI/AAAAAAAACSY/b58685-0gdA/s400/Screenshot_2013-06-06-15-07-33.png
 [3]: https://lh4.googleusercontent.com/-lXsKueC7SLk/UbCNoQM---I/AAAAAAAACUU/h2WsSnw2-M4/s400/Screenshot_2013-06-06-15-24-21.png
 [4]: https://lh6.googleusercontent.com/-pSWHuiYnvqA/UbCPBRZs4gI/AAAAAAAACUk/E06CkPpexZc/s400/Screenshot_2013-06-06-15-29-49.png
