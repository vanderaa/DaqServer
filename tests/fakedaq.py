#! /usr/bin/python
# Fake daq: Just there to test daq chain

import socket
import xmlrpclib
import string
import random
from time import *

# prepare connection to xml server
server_url= 'http://fynulap6/~vanderaa/php/dbinterface.php'
serverxml= xmlrpclib.Server(server_url)


# Prepare querys
while (1):
        print ("---------------------------")
        vvaleur=[]
        for i in range(8):
                vvaleur.append(random.random())
#                print "Valeur",value 
        sleep(2)
        result=serverxml.insertValues(vvaleur)
	print result
s.close()
