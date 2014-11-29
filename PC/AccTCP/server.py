__author__ = 'Kevin Horecka, kevin.horecka@gmail.com'

import socket
import time
import sys
import os
from pyqtgraph.Qt import QtGui, QtCore
import numpy as np
import pyqtgraph as pg
from Filters import *

print "Starting Server..."

serv=socket.socket()

HOST=''
PORT = 12345
BUFSIZE = 4096

print "Host=" + HOST + ", Port=" + str(PORT) + ", Buffer Size=" + str(BUFSIZE)
print "Waiting for Connection..."

serv.bind((HOST, PORT))    
serv.listen(1)  
conn,addr = serv.accept()

print "Connected Successfully."
print "Initializing Graph..."

app = QtGui.QApplication([])

win = pg.GraphicsWindow(title="Accelerometer Monitor")
win.resize(1000,600)
win.setWindowTitle('Accelerometer Monitor')

pg.setConfigOptions(antialias=True)

p = win.addPlot(title="Accelerometer Monitor")
curveX = p.plot(pen='y')
curveY = p.plot(pen='b')
curveZ = p.plot(pen='r')
auto_pan_limit = 100

x = []
y = []
z = []

xraw = []
yraw = []
zraw = []

chunk = ['0,0,0']

filter_window_size = 10
filter_order = 3
filter_carrier_frequency = 2
filter_cutoff_frequency = 0.01

timestr = time.strftime("%Y%m%d-%H%M%S")
path = os.path.dirname(os.path.realpath(__file__))
print path
file = open(path + "\\"+ timestr+".dat", 'w')

no_data_limit = 3000
no_data_prompt = 500
no_data_count = 0

def update():
    global timer, app, conn, serv, data, no_data_count, curveX, curveY, curveZ, file
    data=conn.recv(4096)
    if len(data) > 0:
        if len(x)==auto_pan_limit:
            p.setAutoPan(x=True)
        no_data_count = 0
        chunk=data.split()
        if len(chunk) > 0:
            chunk=chunk[-1].split(',')
            if len(chunk) != 3:
                oldchunk = chunk
                chunk=data.split()
                chunk=chunk[-2].split(',')
                print "Partial Chunk (" + str(oldchunk) + ") Found. Using Previous Sample ("+str(chunk)+")."
            if len(chunk) == 3:
                try:
                    xraw.append(float(chunk[0]))
                    yraw.append(float(chunk[1]))
                    zraw.append(float(chunk[2]))
                    x.append(lowpass(xraw,filter_window_size,filter_cutoff_frequency,filter_carrier_frequency,filter_order))
                    y.append(lowpass(yraw,filter_window_size,filter_cutoff_frequency,filter_carrier_frequency,filter_order))
                    z.append(lowpass(zraw,filter_window_size,filter_cutoff_frequency,filter_carrier_frequency,filter_order))
                    curveX.setData(x)
                    curveY.setData(y)
                    curveZ.setData(z)
                    file.write("("+str(x[-1])+","+str(y[-1])+","+str(z[-1])+")\n")
                    file.flush()
                except ValueError:
                    print "ValueError: chunk="+str(chunk)
            else:
                print "Error: Malformed Data Packet (expected comma separated points, got " + str(data)
        else:
            print "Error: No Data in Chunk (data: " + str(data)
    else:
        no_data_count += 1
    if no_data_count%no_data_prompt == 0 and no_data_count != 0:
        print "Warning: No Data Found for " + str(no_data_count) + " iterations."
    if no_data_count >= no_data_limit:
        print "Error: Connection Probably Closed"
        timer.stop()
        app.quit()

print "Starting Acquisition Loop..."

timer = QtCore.QTimer()
timer.timeout.connect(update)
timer.start(1)

app.exec_()

print "Closing..."

file.close()