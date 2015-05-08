
ixa-pipe-parse
==============
ixa-pipe-parse is a statistical constituent parser for English and Spanish.
ixa-pipe-parse is part of IXA pipes, a set of multilingual NLP tools developed
by the IXA NLP Group [http://ixa2.si.ehu.es/ixa-pipes]. Current version is *1.1.0*.

Please go to [http://ixa2.si.ehu.es/ixa-pipes] for general information about the IXA
pipes tools but also for **official releases, including source code and binary
packages for all the tools in the IXA pipes toolkit**.

This document is intended to be the **usage guide of ixa-pipe-parse**. If you really need to clone
and install this repository instead of using the releases provided in
[http://ixa2.si.ehu.es/ixa-pipes], please scroll down to the end of the document for
the [installation instructions](#installation).

**NOTICE!!**: ixa-pipe-parse is now in [Maven Central](http://search.maven.org/)
for easy access to its API.

## TABLE OF CONTENTS

1. [Overview of ixa-pipe-parse](#overview)
2. [Usage of ixa-pipe-parse](#cli-usage)
  + [Parsing](#parsing)
  + [Evaluation](#evaluation)
3. [API via Maven Dependency](#api)
4. [Git installation](#installation)

## OVERVIEW

ixa-pipe-parse provides:

  + Constituent parsing for English trained on the Penn Treebank and for Spanish trained on the
    [Ancora corpus](http://clic.ub.edu/corpus/ancora). 
  + HeadFinders based on Collins head rules (Michael Collins PhD thesis, 1999).

For this first release we provide two Maximum Entropy models based on a bottom-up shift-reduce method as
described by Adwait Ratnaparkhi (1999). To avoid duplication of efforts, we use the machine learning API
provided by the [Apache OpenNLP project](http://opennlp.apache.org) to train and deploy the models.

Therefore, the following models are provided in the [parse-models.tgz](http://ixa2.si.ehu.es/ixa-pipes/models/parse-models.tgz) package:

* **English Models**:
  + Penn Treebank: **en-parser-chunking.bin**: F1 87.42

+ **Spanish Models**:
  + CoNLL **es-parser-chunking.bin**: F1 88.40

ixa-pipe-parse is distributed under Apache License version 2.0 (see LICENSE.txt for details).

## CLI-Usage

ixa-pipe-parse provides 3 basic functionalities:

1. **parse**: reads a NAF document containing *wf* and *term* elements and
   provides the parsing trees.
2. **train**: trains new model for English or Spanish with several options
   available (**not yet available**).
3. **eval**: functionalities to help evaluating a given model with a given test set.

Each of these functionalities are accessible by adding (parse|train|eval) as a
subcommand to ixa-pipe-parse-$version.jar. Please read below and check the -help
parameter:

````shell
java -jar target/ixa-pipe-parse-$version.jar (parse|train|eval) -help
````

### Parsing

If you are in hurry, just execute:

````shell
cat file.txt | ixa-pipe-tok | ixa-pipe-pos | java -jar $PATH/target/ixa-pipe-parse-$version.jar parse -m model.bin
````

If you want to know more, please follow reading.

ixa-pipe-parse reads NAF documents (with *wf* and *term* elements) via standard input and outputs NAF
through standard output. The NAF format specification is here:

(http://wordpress.let.vupr.nl/naf/)

You can get the necessary input for ixa-pipe-parse by piping
[ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok) and
[ixa-pipe-pos](https://github.com/ixa-ehu/ixa-pipe-pos) as shown in the
example.

There are several options to parse with ixa-pipe-parse:

+ **language**: choose between en and es. If no language is chosen, the one specified
  in the NAF header will be used.
+ **model**: provide the model to do the parsing.
+ **outputFormat**: oneline EVALB format or NAF (the default).
+ **headFinder**: mark constituent headwords using the rules (and variants of) defined in Collins's thesis (1999).

**Example**:

````shell
cat file.txt | ixa-pipe-tok | ixa-pipe-pos | java -jar $PATH/target/ixa-pipe-parse-$version.jar parse -m model.bin
````

### Training new models

This option is in progress, not yet available.

### Evaluation

To evaluate a trained model, the eval subcommand provides the following
options:

+ **language**: input en or es.
+ **model**: input the name of the model to evaluate.
+ **test**: reads a tokenized gold standard and produces the test parse for
  evaluation with EVALB.
+ **headFinder**: mark constituent headwords based on Collins's thesis (1999).
+ **addHeads**: reads directory/file containing oneline treebank
  format trees and annotate the headwords.

**Example**:

````shell
java -jar target/ixa.pipe.parse-$version.jar eval --test gold.tok -l en --model test.bin --nokaf > reference.tree
````

## API

The easiest way to use ixa-pipe-tok programatically is via Apache Maven. Add
this dependency to your pom.xml:

````shell
<dependency>
    <groupId>eus.ixa</groupId>
    <artifactId>ixa-pipe-parse</artifactId>
    <version>1.1.0</version>
</dependency>
````

## JAVADOC

It is possible to generate the javadoc of the module by executing:

````shell
cd ixa-pipe-parse/
mvn javadoc:jar
````

Which will create a jar file core/target/ixa-pipe-parse-$version-javadoc.jar

## Module contents

The contents of the module are the following:

    + formatter.xml           Apache OpenNLP code formatter for Eclipse SDK
    + pom.xml                 maven pom file which deals with everything related to compilation and execution of the module
    + src/                    java source code of the module and required resources
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/                 it contains binary executable and other directories


## INSTALLATION

Installing the ixa-pipe-parse requires the following steps:

If you already have installed in your machine the Java 1.7+ and MAVEN 3, please go to step 3
directly. Otherwise, follow these steps:

### 1. Install JDK 1.7 or JDK 1.8

If you do not install JDK 1.7 in a default location, you will probably need to configure the PATH in .bashrc or .bash_profile:

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

You should now see that your JDK is 1.7

### 2. Install MAVEN 3

Download MAVEN 3 from

````shell
wget http://apache.rediris.es/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz
````

Now you need to configure the PATH. For Bash Shell:

````shell
export MAVEN_HOME=/home/ragerri/local/apache-maven-3.0.5
export PATH=${MAVEN_HOME}/bin:${PATH}
````

For tcsh shell:

````shell
setenv MAVEN3_HOME ~/local/apache-maven-3.0.5
setenv PATH ${MAVEN3}/bin:{PATH}
````

If you re-login into your shell and run the command

````shell
mvn -version
````

You should see reference to the MAVEN version you have just installed plus the JDK 7 that is using.

### 3. Get module source code

If you must get the module source code from here do this:

````shell
git clone https://github.com/ixa-ehu/ixa-pipe-parse
````

### 4. Dowload the Models

Download and untar the models:

````shell
wget http://ixa2.si.ehu.es/ixa-pipes/models/parse-models.tgz
tar xvzf parse-models.tgz
````
### 5. Compile

````shell
cd ixa-pipe-parse
mvn clean package
````

This step will create a directory called target/ which contains various directories and files.
Most importantly, there you will find the module executable:

ixa-pipe-parse-$version.jar

This executable contains every dependency the module needs, so it is completely portable as long
as you have a JVM 1.7 installed.

To install the module in the local maven repository, usually located in ~/.m2/, execute:

````shell
mvn clean install
````

## Contact information

````shell
Rodrigo Agerri
IXA NLP Group
University of the Basque Country (UPV/EHU)
E-20018 Donostia-San Sebastián
rodrigo.agerri@ehu.eus
````
