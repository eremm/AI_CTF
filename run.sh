#!/bin/bash
if javac -cp . ctf/agent/ecr110030Agent.java ; then
	java -cp . ctf.environment.TestPlaySurface
fi
