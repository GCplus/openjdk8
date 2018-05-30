自述文件（关于）：  
该文件应该位于OpenJDK的Mercurial存储库的开始。完整的OpenJDK存储库集（林）还应该包括以下6个顺序嵌套的存储库：
+ jdk
+ hotspot
+ langtools
+ corba
+ jaxws
+ jaxp

根存储库可以通过以下方式下载获得：
```
hg clone http://hg.openjdk.java.net/jdk8/jdk8 openjdk8
```  

您可以运行位于存储库最开始的get_source.sh脚本，以获得所需的其他版本的存储库：
```
    cd openjdk8 && sh ./get_source.sh
```

不熟悉Mercurial的人可以去读一下  Mercurial book 的前几章:
```
   http://hgbook.red-bean.com/read/
```
关注[http://openjdk.java.net/](http://openjdk.java.net/) 网址来获取更多关于OpenJDK的更多信息。

简单构造OpenJDK的步骤：  

1. 在系统上安装必要的系统软件/软件包，请参考
http://hg.openjdk.java.net/jdk8/jdk8/raw-file/tip/README-builds.html

2. 如果你没有JDK7或者更新的版本，请从[ http://java.sun.com/javase/downloads/index.jsp]( http://java.sun.com/javase/downloads/index.jsp)网址进行下载，并且将/bin添加到你的PATH环境变量里面。
3. 配置构建环境：
 ```
 bash ./configure
 ```
4. 构建 OpenJDK：
 ```
 make all
 ```
 生成的JDK镜像应该在
 ```
  build/*/images/j2sdk-image
 ```
当你的make是GNU make 3.81或者更新的版本，通常是运行在linux 3.81或者更新的内核上。注意，在Solaris，GNU make被命名为“gmake”。

以下文件有完整的细节：
http://hg.openjdk.java.net/jdk8/jdk8/raw-file/tip/README-builds.html
