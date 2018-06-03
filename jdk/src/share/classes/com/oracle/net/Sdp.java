/*
 *版权所有（c）1995,2013，Oracle和/或其附属公司。版权所有。
 *请勿更改或删除版权声明或本文件头。
 *
 *此代码是免费软件;你可以重新分配和/或修改它
 *仅限于GNU通用公共许可证版本2的条款，如
 *由自由软件基金会发布。 Oracle指定了这一点
 *特定文件受限于所提供的“Classpath”异常
 *由Oracle在伴随此代码的LICENSE文件中提供。
 *
 *这个代码是分发的，希望它会有用，但没有
 *任何担保;甚至没有对适销性或适销性的暗示保证
 *针对特定用途的适用性。请参阅GNU通用公共许可证
 *版本2了解更多详情（一份副本包含在LICENSE文件中
 *附有此代码）。
 *
 *您应该收到GNU通用公共许可证版本的副本
 * 2与这项工作一起;如果没有，请写信给自由软件基金会，
 * Inc.，51 Franklin St，Fifth Floor，Boston，MA 02110-1301 USA。
 *
 *请联系Oracle，500 Oracle Parkway，Redwood Shores，CA 94065 USA
 *或访问www.oracle.com如果你需要更多的信息或有任何
 *问题。
 */

package com.oracle.net;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.io.IOException;
import java.io.FileDescriptor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.lang.reflect.Constructor;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;

import sun.net.sdp.SdpSupport;

/**
 * 该类完全由静态方法支持套接字或通道，
 * 以支持无限带宽（InfiniBand）套接字直接协议（SDP）。 
 */

public final class Sdp {
    private Sdp() { }

    /**
     * 私有包(package-privage) ServerSocket(SocketImpl) 的 构造函数
     */
    private static final Constructor<ServerSocket> serverSocketCtor;
    static {
        try {
            serverSocketCtor = (Constructor<ServerSocket>)
                ServerSocket.class.getDeclaredConstructor(SocketImpl.class);
            setAccessible(serverSocketCtor);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * The package-private SdpSocketImpl() constructor
     */
    private static final Constructor<SocketImpl> socketImplCtor;
    static {
        try {
            Class<?> cl = Class.forName("java.net.SdpSocketImpl", true, null);
            socketImplCtor = (Constructor<SocketImpl>)cl.getDeclaredConstructor();
            setAccessible(socketImplCtor);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static void setAccessible(final AccessibleObject o) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                o.setAccessible(true);
                return null;
            }
        });
    }

    /**
     * SDP enabled Socket.
     */
    private static class SdpSocket extends Socket {
        SdpSocket(SocketImpl impl) throws SocketException {
            super(impl);
        }
    }

    /**
     * Creates a SDP enabled SocketImpl
     */
    private static SocketImpl createSocketImpl() {
        try {
            return socketImplCtor.newInstance();
        } catch (InstantiationException x) {
            throw new AssertionError(x);
        } catch (IllegalAccessException x) {
            throw new AssertionError(x);
        } catch (InvocationTargetException x) {
            throw new AssertionError(x);
        }
    }

    /**
     * Creates an unconnected and unbound SDP socket. The {@code Socket} is
     * associated with a {@link java.net.SocketImpl} of the system-default type.
     *
     * @return  a new Socket
     *
     * @throws  UnsupportedOperationException
     *          If SDP is not supported
     * @throws  IOException
     *          If an I/O error occurs
     */
    public static Socket openSocket() throws IOException {
        SocketImpl impl = createSocketImpl();
        return new SdpSocket(impl);
    }

    /**
     * Creates an unbound SDP server socket. The {@code ServerSocket} is
     * associated with a {@link java.net.SocketImpl} of the system-default type.
     *
     * @return  a new ServerSocket
     *
     * @throws  UnsupportedOperationException
     *          If SDP is not supported
     * @throws  IOException
     *          If an I/O error occurs
     */
    public static ServerSocket openServerSocket() throws IOException {
        // create ServerSocket via package-private constructor
        SocketImpl impl = createSocketImpl();
        try {
            return serverSocketCtor.newInstance(impl);
        } catch (IllegalAccessException x) {
            throw new AssertionError(x);
        } catch (InstantiationException x) {
            throw new AssertionError(x);
        } catch (InvocationTargetException x) {
            Throwable cause = x.getCause();
            if (cause instanceof IOException)
                throw (IOException)cause;
            if (cause instanceof RuntimeException)
                throw (RuntimeException)cause;
            throw new RuntimeException(x);
        }
    }

    /**
     * Opens a socket channel to a SDP socket.
     *
     * <p> The channel will be associated with the system-wide default
     * {@link java.nio.channels.spi.SelectorProvider SelectorProvider}.
     *
     * @return  a new SocketChannel
     *
     * @throws  UnsupportedOperationException
     *          If SDP is not supported or not supported by the default selector
     *          provider
     * @throws  IOException
     *          If an I/O error occurs.
     */
    public static SocketChannel openSocketChannel() throws IOException {
        FileDescriptor fd = SdpSupport.createSocket();
        return sun.nio.ch.Secrets.newSocketChannel(fd);
    }

    /**
     * Opens a socket channel to a SDP socket.
     *
     * <p> The channel will be associated with the system-wide default
     * {@link java.nio.channels.spi.SelectorProvider SelectorProvider}.
     *
     * @return  a new ServerSocketChannel
     *
     * @throws  UnsupportedOperationException
     *          If SDP is not supported or not supported by the default selector
     *          provider
     * @throws  IOException
     *          If an I/O error occurs
     */
    public static ServerSocketChannel openServerSocketChannel()
        throws IOException
    {
        FileDescriptor fd = SdpSupport.createSocket();
        return sun.nio.ch.Secrets.newServerSocketChannel(fd);
    }
}
