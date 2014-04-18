
ixa-pipe-parse
==============
ixa-pipe-parse is a probabilistic constituent parser for English and Spanish. 
ixa-pipe-parse is part of IXA Pipeline ("is a pipeline"), a multilingual NLP pipeline developed 
by the IXA NLP Group [http://ixa2.si.ehu.es/ixa-pipes]. 

Please go to [http://ixa2.si.ehu.es/ixa-pipes] for general information about the IXA
pipeline tools but also for **official releases, including source code and binary
packages for all the tools in the IXA pipeline**.

This document is intended to be the **usage guide of ixa-pipe-parse**. If you really need to clone
and install this repository instead of using the releases provided in
[http://ixa2.si.ehu.es/ixa-pipes], please scroll down to the end of the document for
the [installation instructions](#installation).

## OVERVIEW 

ixa-pipe-parse provides:

  + Constituent parsing for English trained on the Penn Treebank and for Spanish trained on the
    [Ancora corpus](http://clic.ub.edu/corpus/ancora). 
  + HeadFinders based on Collins head rules (Michael Collins PhD thesis, 1999) and
    Stanford's parser Semantic Head Rules. 

For this first release we provide two reasonably fast Maximum Entropy models based on a bottom-up shift-reduce method as 
described by Adwait Ratnaparkhi (1999). To avoid duplication of efforts, we use the machine learning API 
provided by the [Apache OpenNLP project](http://opennlp.apache.org).

Therefore, the following models are provided in the [parse-resources.tgz](http://ixa2.si.ehu.es/ixa-pipes/models/parse-resources.tgz) package: 

* **English Models**:
  + Penn Treebank: **en-parser-chunking.bin**: F1 87.42

+ **Spanish Models**: 
  + CoNLL **es-parser-chunking.bin**: F1 88.40

ixa-pipe-parse is distributed under Apache License version 2.0 (see LICENSE.txt for details).

## USING ixa-pipe-parse

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

### Parsing with ixa-pipe-parse

If you are in hurry, just execute: 

````shell
cat file.txt | ixa-pipe-tok | ixa-pipe-pos | java -jar $PATH/target/ixa-pipe-parse-$version.jar parse
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

+ **lang**: choose between en and es. If no language is chosen, the one specified
  in the NAF header will be used.
+ **features**: choose features to use during the decoding. Currently only one feature
  types is provided: 
  + **baseline**: it implements the features described in Ratnapharki (1999). 
+ **model**: provide the model to do the parsing. If no model is provided via
  this parameter, ixa-pipe-parse will revert to the baseline model distributed
  in the parse-resources.tgz. 
+ **beamsize**: choose beam size for decoding. There is no definitive evidence
  that using larger or smaller beamsize actually improves accuracy. It is known
  to slow things down considerably if beamsize is set to 100, for example.
+ **nokaf**: switch to output parse trees in NAF or Penn Treebank oneline
  format.
+ **heads**: mark constituent headwords. Two methods are available:
  + **collins**: Head rules as defined in Collins's thesis (1999).
  + **sem**: Semantic head rules defined by the Stanford parser.

**Example**: 

````shell
cat file.txt | ixa-pipe-tok | ixa-pipe-pos | java -jar $PATH/target/ixa-pipe-parse-$version.jar parse
````

### Training new models

This option is in progress, not yet available.

### Evaluation

To evaluate a trained model, the eval subcommand provides the following
options: 

+ **language**: input en or es.
+ **model**: input the name of the model to evaluate.
+ **features**: currently only default baseline available.
+ **test**: reads a tokenized gold standard and produces the test parse for
  evaluation with evalb.
+ **nokaf**: switch to output parse trees in NAF or Penn Treebank oneline
  format.
+ **heads**: mark constituent headwords. Two methods are available:
  + **collins**: Head rules as defined in Collins's thesis (1999).
  + **sem**: Semantic head rules defined by the Stanford parser.
+ **processTreebankWithHeadWords**: reads directory/file containing oneline treebank
  format trees and annotate the headwords.
+ **extension**:Specify extension of files, e.g. '.txt' or '' for every file to be
  processed by the *processTreebankWithHeadWords* option.  

**Example**:

````shell
java -jar target/ixa.pipe.parse-$version.jar eval --test gold.tok -l en --model test.bin --nokaf > reference.tree
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

### 1. Install JDK 1.7

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

### 4. Download the Resources

You will need to download the trained models and other resources and copy them to ixa-pipe-parse/src/main/resources/
for the module to work properly:

Download the models and untar the archive into the src/main/resources directory:

````shell
cd ixa-pipe-parse/src/main/resources
wget http://ixa2.si.ehu.es/ixa-pipes/models/parse-resources.tgz
tar xvzf parse-resources.tgz
````
The parse-resources contains the baseline models to which ixa-pipe-parse backs off if not model is provided as parameter
for tagging.

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
rodrigo.agerri@ehu.es
````
