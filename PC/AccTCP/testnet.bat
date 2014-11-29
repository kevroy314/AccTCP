netsh wlan set hostednetwork mode=allow ssid=test key=coolness
netsh wlan start hostednetwork
pause
ipconfig
pause
python %0\..\server.py
pause