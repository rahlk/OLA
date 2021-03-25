### Setup

* Clone repo

* Install souffle using following commands (modify config.ac to modify install directory)
    
      sudo apt-get install autoconf automake bison build-essential clang doxygen flex g++ git libncurses5-dev libtool libsqlite3-dev make mcpp python sqlite zlib1g-dev
      cd souffle
      sh ./bootstrap
      ./configure
      make
      PATH=$PATH:<install-dir>/bin

* Change to doop-framework (not necessary to put souffle and doop in same directory)

* Run (can also use example.jar provided in docs/doop-101-examples as a starter) 

      ./doop -i dependency-analysis -a ../irs.jar    
