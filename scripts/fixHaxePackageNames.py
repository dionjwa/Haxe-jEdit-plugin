#! /usr/bin/env python
"""
Repairs HaXe package names.
"""
import os.path, string, sys, glob, re

pattern = re.compile('^\\s*package .*')


if len(sys.argv) == 1:
    print "Usage: fixHaxePackageNames.py <src folder>"
    print "Repairs HaXe source package names"
    sys.exit(0)


path = sys.argv[1]

path = os.path.join(path, "")

def packageFromFileName (fileName):
    tokens = fileName.strip().split("/")[0:-1]
    return string.join(tokens, ".")

fileSet = set()

for root, dirs, files in os.walk(path):
    for fileName in files:
        if fileName.endswith(".hx"):
            fileSet.add( os.path.join( root[len(path):], fileName ))

packagesOk = True
for fileName in fileSet:
    correctedFileName = os.path.join(path, fileName)
    f = open(correctedFileName, 'r')
    lines = f.readlines()
    f.close()

    write = False
    for i in range(len(lines)):
        line = lines[i]
        if pattern.match(line):
            packageString = line.replace("package", "")
            packageString = packageString.replace(";", "")
            packageString = packageString.strip()

            filePackage = packageFromFileName(fileName)


            if packageString != filePackage:
                write = True
                lines[i] = "package " + filePackage + ";\n"
                print fileName +": " + packageString + " -> " + filePackage
                packagesOk = False
            break
    if write:
        f = open(correctedFileName, 'w')
        for line in lines:
            f.write(line)
        f.close()

if packagesOk:
    print "Package structure ok."
