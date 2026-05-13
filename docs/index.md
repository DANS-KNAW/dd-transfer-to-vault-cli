dd-transfer-to-vault-cli
===========

<!-- Remove this comment and extend the descriptions below -->


SYNOPSIS
--------

    dd-transfer-to-vault-cli { server | check }


DESCRIPTION
-----------

CLI for dd-transfer-to-vault


ARGUMENTS
---------

        positional arguments:
        {server,check}         available commands
        
        named arguments:
        -h, --help             show this help message and exit
        -v, --version          show the application version and exit

EXAMPLES
--------

<!-- Add examples of invoking this module from the command line or via HTTP other interfaces -->
    

INSTALLATION AND CONFIGURATION
------------------------------
Currently this project is built as an RPM package for RHEL7/CentOS7 and later. The RPM will install the binaries to
`/opt/dans.knaw.nl/dd-transfer-to-vault-cli` and the configuration files to `/etc/opt/dans.knaw.nl/dd-transfer-to-vault-cli`. 

BUILDING FROM SOURCE
--------------------
Prerequisites:

* Java 11 or higher
* Maven 3.3.3 or higher
* RPM

Steps:
    
    git clone https://github.com/DANS-KNAW/dd-transfer-to-vault-cli.git
    cd dd-transfer-to-vault-cli 
    mvn clean install
