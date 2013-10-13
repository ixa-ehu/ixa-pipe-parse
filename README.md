
IXA-pipe-parse
=================

This module is part of IXA-Pipeline ("is a pipeline"), a multilingual NLP pipeline
developed by the IXA NLP Group (ixa.si.ehu.es).

Ixa-pipe-parse provides:
   - Constituent parsing for English and Spanish.
   - HeadFinders based on Collins head rules (Michael Collins PhD thesis, 1999) and
     Stanford's parser Semantic Head Rules.
   - Outputs to KAF by default (https://github.com/opener-project/kaf/wiki/KAF-structure-overview)
     but also in Penn Treebank oneline style.

English models have been trained using the Penn treebank and Spanish models
using the Ancora corpus (http://clic.ub.edu/corpus/en/ancora) and the Apache OpenNLP API.
The training is based on Maximum Entropy models for parsing as described by Adwait Ratnaparkhi's PhD thesis (1998).

ixa-pipe-parse development has been partially funded by the OpeNER FP7 European Project (http://www.opener-project.org),
under grant agreement No. 296451.

ixa-pipe-parse is distributed under Apache License version 2.0 (see LICENSE.txt for details).

Contents
========

The contents of the module are the following:

    + formatter.xml           Apache OpenNLP code formatter for Eclipse SDK
    + pom.xml                 maven pom file which deals with everything related to compilation and execution of the module
    + src/                    java source code of the module
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/                 it contains binary executable and other directories


INSTALLATION
============

Installing the ixa-pipe-parse requires the following steps:

If you already have installed in your machine JDK7 and MAVEN 3, please go to step 3
directly. Otherwise, follow these steps:

1. Install JDK 1.7
-------------------

If you do not install JDK 1.6 in a default location, you will probably need to configure the PATH in .bashrc or .bash_profile:

````shell
export JAVA_HOME=/yourpath/local/java7
export PATH=${JAVA_HOME}/bin:${PATH}
````

If you use tcsh you will need to specify it in your .login as follows:

````shell
setenv JAVA_HOME /usr/java/java17
setenv PATH ${JAVA_HOME}/bin:${PATH}
````

If you re-login into your shell and run the command

````shell
java -version
````

You should now see that your jdk is 1.7

2. Install MAVEN 3
------------------

Download MAVEN 3 from

````shell
wget http://www.apache.org/dyn/closer.cgi/maven/maven-3/3.0.4/binaries/apache-maven-3.0.4-bin.tar.gz
````

Now you need to configure the PATH. For Bash Shell:

````shell
export MAVEN_HOME=/home/ragerri/local/apache-maven-3.0.4
export PATH=${MAVEN_HOME}/bin:${PATH}
````

For tcsh shell:

````shell
setenv MAVEN3_HOME ~/local/apache-maven-3.0.4
setenv PATH ${MAVEN3}/bin:{PATH}
````

If you re-login into your shell and run the command

````shell
mvn -version
````

You should see reference to the MAVEN version you have just installed plus the JDK 6 that is using.

3. Get module source code
--------------------------

````shell
git clone git@github.com:ixa-ehu/ixa-pipe-parse.git
````

4. Download trained models
--------------------------

You will need to download the trained parser models for the module to work properly. Download the models
and untar the archive into the src/main/resources directory:

````shell
cd ixa-pipe-parse/src/main/resources
wget http://ixa2.si.ehu.es/ragerri/ixa-pipeline-models/parser-resources.tgz
tar xvzf parser-resources.tgz
````
Note that if you change the name of the models you will need to modify also the source code in Models.java.

5. Move into main directory
---------------------------

````shell
cd ixa-pipe-parse
````

6. Install module using maven
-----------------------------

````shell
mvn clean package
````

This step will create a directory called target/ which contains various directories and files.
Most importantly, there you will find the module executable:

ixa-pipe-parse-1.0.jar

This executable contains every dependency the module needs, so it is completely portable as long
as you have a JVM 1.7 installed.

To install the module in the local maven repository, usually located in ~/.m2/, execute:

````shell
mvn clean install
````

7. USING ixa-pipe-parse
==========================

The program takes tokenized and POS-tagged text in KAF form (e.g., <text> and <terms> elements)
as standard input and outputs syntactic analysis (both in KAF <constituents> or in Treebank format into
standard output.

https://github.com/opener-project/kaf/wiki/KAF-structure-overview

The tokenized and POS-tagged input KAF in <text> and <terms> KAF elements can be obtained by piping
ixa-pipe-tok (http://github.com/ixa-ehu/ixa-pipe-tok) and ixa-pipe-pos (http://github.com/ixa-pipe-pos) or
by providing an already processed document containing this information.

To run the program execute:

````shell
cat infile.kaf | java -jar $PATH/target/ixa-pipe-parse-1.0.jar
````
This will output the constituent parse tree into a KAF document.

Run the parser with the -help parameter for a description of other available options.

````shell
java -jar $PATH/target/ixa-pipe-parse-1.0.jar -help
````

GENERATING JAVADOC
==================

You can also generate the javadoc of the module by executing:

````shell
mvn javadoc:jar
````

Which will create a jar file core/target/ixa-pipe-parse-1.0-javadoc.jar


Contact information
===================

````shell
Rodrigo Agerri
IXA NLP Group
University of the Basque Country (UPV/EHU)
E-20018 Donostia-San Sebastián
rodrigo.agerri@ehu.es
````
