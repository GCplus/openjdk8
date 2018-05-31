#!/bin/sh

#
# 版权所有（c）2012,2013，Oracle和/或其附属公司。版权所有。
# 请勿更改或删除版权声明或本文件头。
#
# 此代码是免费软件;你可以重新分配和/或修改它
# 仅限于GNU通用公共许可证版本2的条款，如
# 由自由软件基金会发布。 Oracle指定了这一点
# 特定文件受限于所提供的“Classpath”异常
# 由甲骨文在附带此代码的LICENSE文件中提供。
# 
# 这个代码是分发的，希望它会有用，但没有
# 任何担保;甚至没有对适销性或适销性的暗示保证
# 针对特定用途的适用性。请参阅GNU通用公共许可证
# 版本2了解更多详情（一份副本包含在LICENSE文件中
# 附上此代码）。
# 
# 您应该收到GNU通用公共许可证版本的副本
# 2连同这项工作;如果没有，请写信给自由软件基金会，
# Inc.，51 Franklin St，Fifth Floor，Boston，MA 02110-1301 USA。
# 
# 请联系Oracle，500 Oracle Parkway，Redwood Shores，CA 94065 USA
# 或访问www.oracle.com，如果你需要更多的信息或有任何
# 问题。
#

# 该函数是将所有的报错内容输出出来
to_stderr() {
    echo "$@" >&2
}

error() {
    to_stderr "ERROR: $1"
    exit ${2:-126}
}

warning() {
    to_stderr "WARNING: $1"
}

version_field() {
  # rev is typically omitted for minor and major releases
  field=`echo ${1}.0 | cut -f ${2} -d .`
  if expr 1 + $field >/dev/null 2> /dev/null; then
    echo $field
  else
    echo -1
  fi
}

# Version check
# 版本检查
# required
reqdmajor=1
reqdminor=4
reqdrev=0

# requested
rqstmajor=2
rqstminor=6
rqstrev=3


# installed
hgwhere="`command -v hg`"
if [ "x$hgwhere" = "x" ]; then
  error "Could not locate Mercurial command"
fi

hgversion="`LANGUAGE=en hg --version 2> /dev/null | sed -n -e 's@^Mercurial Distributed SCM (version \([^+]*\).*)\$@\1@p'`"
if [ "x${hgversion}" = "x" ] ; then
  error "Could not determine Mercurial version of $hgwhere"
fi

hgmajor="`version_field $hgversion 1`"
hgminor="`version_field $hgversion 2`"
hgrev="`version_field $hgversion 3`"

if [ $hgmajor -eq -1 -o $hgminor -eq -1 -o $hgrev -eq -1 ] ; then
  error "Could not determine Mercurial version of $hgwhere from \"$hgversion\""
fi


# Require
if [ $hgmajor -lt $reqdmajor -o \( $hgmajor -eq $reqdmajor -a $hgminor -lt $reqdminor \) -o \( $hgmajor -eq $reqdmajor -a $hgminor -eq $reqdminor -a $hgrev -lt $reqdrev \) ] ; then
  error "Mercurial version $reqdmajor.$reqdminor.$reqdrev or later is required. $hgwhere is version $hgversion"
fi


# Request
if [ $hgmajor -lt $rqstmajor -o \( $hgmajor -eq $rqstmajor -a $hgminor -lt $rqstminor \) -o \( $hgmajor -eq $rqstmajor -a $hgminor -eq $rqstminor -a $hgrev -lt $rqstrev \) ] ; then
  warning "Mercurial version $rqstmajor.$rqstminor.$rqstrev or later is recommended. $hgwhere is version $hgversion"
fi


# Get clones of all absent nested repositories (harmless if already exist)
sh ./common/bin/hgforest.sh clone "$@" || exit $?

# Update all existing repositories to the latest sources
sh ./common/bin/hgforest.sh pull -u
