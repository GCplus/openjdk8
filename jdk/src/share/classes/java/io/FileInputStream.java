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

package java.io;

import java.nio.channels.FileChannel;
import sun.nio.ch.FileChannelImpl;


/**
 * <code>FileInputStream</code>从文件系统中的文件获取输入字节码。
 * 什么文件可用取决于主机环境。
 *
 * <p><code>FileInputStream </code>用于读取诸如图像数据之类的原始字节流。
 *  为了方便阅读字符流，请考虑使用
 * <code> FileReader </code>。
 *
 * @author  Arthur van Hoff
 * @see     java.io.File
 * @see     java.io.FileDescriptor
 * @see     java.io.FileOutputStream
 * @see     java.nio.file.Files#newInputStream
 * @since   JDK1.0
 */
public class FileInputStream extends InputStream
{
    /* 文件描述符(Descriptor) --(handle)处理打开的文件 */
    private final FileDescriptor fd;

    /**
     * 引用文件(referenced file)的路径
     * （如果流(stream)是用文件描述符(file descriptor)创建的，则可以为空）
     */
    private final String path;

    private FileChannel channel = null;

    private final Object closeLock = new Object();
    private volatile boolean closed = false;

    /**
     * 通过打开与实际文件的连接来创建<code> FileInputStream </code>，
     * 文件的命名是通过在文件系统的路径名字<code>name</code>获取。
     * 将创建一个新的<code> FileDescriptor </ code>对象来表示此文件连接。
     * <p>
     * 首先，如果存在安全管理器(security manager)，
     * 则使用<code> checkRead </code>方法调用其<code> name </code>参数作为参数。
     * <p>
     * 如果指定的文件名字不存在，则该名字是目录(directory)而不是常规文件(regular file)，
     * 或者由于某些其他原因无法打开读取，则抛出<code> FileNotFoundException </code>。
     *
     * @param      name   该系统有关的文件名(the system-dependent file name)
     * @exception  FileNotFoundException  如果该文件不存在，
     *                 或是一个目录而不是一个普通的文件，
     *                 或由于其他原因无法打开阅读。
     * @exception  SecurityException      如果安全管理器存在
     *                 并且系统拒绝了<code> checkRead </code>方法的读取文件的权限。
     * @see        java.lang.SecurityManager#checkRead(java.lang.String)
     */
    public FileInputStream(String name) throws FileNotFoundException {
        this(name != null ? new File(name) : null);
    }

    /**
     * 通过打开一个与实际文件连接创建一个 <code>FileInputStream</code>，
     * 这个文件名字是通过文件系统中的<code>File</code>对象<code>file</code>来读取的。
     * 创建一个新的<code> FileDescriptor </code>对象来表示此文件连接。
     * <p>
     * 首先，如果存在安全管理器，
     * 则使用由<code>file</code>参数表示的路径作为其参数调用<code> checkRead </code>方法。
     * <p>
     * 如果指定的文件不存在，是目录而不是常规文件，
     * 或者由于某些其他原因无法读取，
     * 则抛出<code> FileNotFoundException </code>异常。
     *
     * @param      file   该文件将被打开读取
     * @exception  FileNotFoundException  如果指定的文件不存在，
     *                    是目录而不是常规文件，
     *                    或者由于某些其他原因无法读取
     * @exception  SecurityException      如果安全管理器存在
     *                    并且系统拒绝了<code> checkRead </code>方法的读取文件的权限。
     * @see        java.io.File#getPath()
     * @see        java.lang.SecurityManager#checkRead(java.lang.String)
     */
    public FileInputStream(File file) throws FileNotFoundException {
        String name = (file != null ? file.getPath() : null);
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkRead(name);
        }
        if (name == null) {
            throw new NullPointerException();
        }
        if (file.isInvalid()) {
            throw new FileNotFoundException("Invalid file path");
        }
        fd = new FileDescriptor();
        fd.attach(this);
        path = name;
        open(name);
    }

    /**
     * 使用文件描述符<code>fdObj</code>创建一个<code>FileInputStream</code>，
     * 该文件描述符表示与文件系统中实际文件的现有连接。
     * <p>
     * 如果存在安全管理器，
     * 则使用文件描述符<code>fdObj</code>作为其参数调用<code>checkRead</code>方法，
     * 以查看是否可以读取文件描述符。
     * 如果文件描述符的读权限被拒绝，则抛出<code>SecurityException</code>异常
     * <p>
     * 如果<code> fdObj </code>为null，则引发<code> NullPointerException </code>异常。
     * <p>
     * 如果<code>fdObj</code>是{@link java.io.FileDescriptor#valid() invalid}，
     * 那么该方法不会抛出异常。
     * 但是，如果在生成的流上调用此方法以在流上尝试I/O，则会引发<code>IOException</code>异常。
     *
     * @param      fdObj   打开文件描述符以供阅读(the file descriptor to be opened for reading)
     * @throws     SecurityException      如果安全管理器存在
     *                     并且系统拒绝了<code> checkRead </code>方法的读取文件描述符权限
     * @see        SecurityManager#checkRead(java.io.FileDescriptor)
     */
    public FileInputStream(FileDescriptor fdObj) {
        SecurityManager security = System.getSecurityManager();
        if (fdObj == null) {
            throw new NullPointerException();
        }
        if (security != null) {
            security.checkRead(fdObj);
        }
        fd = fdObj;
        path = null;

        /*
         * 文件描述符(FileDescriptor)正在被流(streams)共享。
         * 使用文件描述符(FileDescriptor)跟踪器(tracker)注册此流。
         */
        fd.attach(this);
    }

    /**
     * 打开指定的文件以进行读取
     * @param name 文件的名字
     */
    private native void open0(String name) throws FileNotFoundException;

    // wrap native call to allow instrumentation（包装本地调用以允许检测）
    /**
     * 打开指定的文件以进行读取。
     * @param name 文件的名字
     */
    private void open(String name) throws FileNotFoundException {
        open0(name);
    }

    /**
     * 从这个输入流读取一个字节的数据。如果没有输入，则此方法会阻塞。
     *
     * @return     数据的下一个字节，如果到达文件的末尾，则为<code> -1 </code>。
     * @exception  IOException  如果发生I/O错误。
     */
    public int read() throws IOException {
        return read0();
    }

    private native int read0() throws IOException;

    /**
     * 将子数组(subarray)作为字节序列读取(sequence of bytes)。
     * @param b 要写入的数据
     * @param off 数据中的起始偏移量
     * @param len 写入的字节数
     * @exception IOException 如果发生I/O错误。
     */
    private native int readBytes(byte b[], int off, int len) throws IOException;

    /**
     * 从这个输入流中读取<code> b.length </code>字节的数据到一个字节数组中。此方法阻塞，直到有些输入可用。
     *
     * @param      b   数据读入的缓冲区。
     * @return     由于已经达到了文件的末尾而没有更多的数据，则读入缓冲区的总字节数或<code> -1 </code>
     * @exception  IOException  如果发生I/O异常
     */
    public int read(byte b[]) throws IOException {
        return readBytes(b, 0, b.length);
    }

    /**
     * 从该输入流中将<code> len </ code>字节的数据读入到一个字节数组中。
     * 如果<code> len </code>不为零，该方法会阻塞，直到某些输入可用; 
     * 否则，不读取字节并返回<code> 0 </code>。
     * 
     * @param      b     数据读入的缓冲区。
     * @param      off   目标数组<code> b </code>中的起始偏移量
     * @param      len   读取的最大字节数。
     * @return     如果由于已达到文件末尾而没有更多数据，
     *             则读入缓冲区的总字节数或<code> -1 </ code>。
     * @exception  NullPointerException 如果 <code>b</code> 是 <code>null</code>.
     * @exception  IndexOutOfBoundsException 如果<code> off </code>为负数，
     *             <code> len </code>为负数，
     *             或者<code> len </code>大于<code> b.length - off </code>
     * @exception  IOException  如果发生I/O错误。
     */
    public int read(byte b[], int off, int len) throws IOException {
        return readBytes(b, off, len);
    }

    /**
     * 跳过并丢弃输入流中的<code> n </code>字节数据。
     *
     * <p>
     * 由于各种原因，<code> skip </code>方法可能会跳过一些较小数量的字节，
     * 可能是<code> 0 </code>。 如果<code> n </code>是负数，
     * 该方法将尝试向后跳过。 如果后备文件在当前位置不支持向后跳转，
     * 则抛出<code> IOException </code>。 返回跳过的实际字节数。
     * 如果向前跳过，则返回正值。 如果它向后跳，它会返回一个负值。
     *
     * <p>
     * 此方法可能会跳过比备份文件(backing file)中剩余的更多的字节。
     * 这不会产生异常，并且跳过的字节数可能包含超出备份文件EOF的一些字节数。
     * 在跳过流的结尾后尝试从流中读取数据将导致 -1 ，这代表已经读取到了文件的结尾(
     * Attempting to read from the stream after skipping past
     * the end will result in -1 indicating the end of the file)。
     * 
     * @param      n   要跳过的字节数
     * @return     实际跳过的字节数
     * @exception  IOException  如果n是负数，
     *             如果流不支持查找，或者发生I/O错误。
     */
    public long skip(long n) throws IOException {
        return skip0(n);
    }

    private native long skip0(long n) throws IOException;

    /**
     * Returns an estimate of the number of remaining bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. Returns 0 when the file
     * position is beyond EOF. The next invocation might be the same thread
     * or another thread. A single read or skip of this many bytes will not
     * block, but may read or skip fewer bytes.
     * 返回可从此输入流中读取（或跳过）的剩余字节数的估计值，
     * 而不会因为此输入流的下一次方法调用而被阻塞。
     * 文件位置超出EOF时返回0。 下一次调用可能是同一个线程或另一个线程。
     * 单个读取或跳过这么多字节不会被阻塞，但可以读取或跳过更少的字节。
     *
     * <p> In some cases, a non-blocking read (or skip) may appear to be
     * blocked when it is merely slow, for example when reading large
     * files over slow networks.
     *
     * @return     an estimate of the number of remaining bytes that can be read
     *             (or skipped over) from this input stream without blocking.
     * @exception  IOException  if this file input stream has been closed by calling
     *             {@code close} or an I/O error occurs.
     */
    public int available() throws IOException {
        return available0();
    }

    private native int available0() throws IOException;

    /**
     * Closes this file input stream and releases any system resources
     * associated with the stream.
     *
     * <p> If this stream has an associated channel then the channel is closed
     * as well.
     *
     * @exception  IOException  if an I/O error occurs.
     *
     * @revised 1.4
     * @spec JSR-51
     */
    public void close() throws IOException {
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        if (channel != null) {
           channel.close();
        }

        fd.closeAll(new Closeable() {
            public void close() throws IOException {
               close0();
           }
        });
    }

    /**
     * Returns the <code>FileDescriptor</code>
     * object  that represents the connection to
     * the actual file in the file system being
     * used by this <code>FileInputStream</code>.
     *
     * @return     the file descriptor object associated with this stream.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FileDescriptor
     */
    public final FileDescriptor getFD() throws IOException {
        if (fd != null) {
            return fd;
        }
        throw new IOException();
    }

    /**
     * Returns the unique {@link java.nio.channels.FileChannel FileChannel}
     * object associated with this file input stream.
     *
     * <p> The initial {@link java.nio.channels.FileChannel#position()
     * position} of the returned channel will be equal to the
     * number of bytes read from the file so far.  Reading bytes from this
     * stream will increment the channel's position.  Changing the channel's
     * position, either explicitly or by reading, will change this stream's
     * file position.
     *
     * @return  the file channel associated with this file input stream
     *
     * @since 1.4
     * @spec JSR-51
     */
    public FileChannel getChannel() {
        synchronized (this) {
            if (channel == null) {
                channel = FileChannelImpl.open(fd, path, true, false, this);
            }
            return channel;
        }
    }

    private static native void initIDs();

    private native void close0() throws IOException;

    static {
        initIDs();
    }

    /**
     * Ensures that the <code>close</code> method of this file input stream is
     * called when there are no more references to it.
     *
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FileInputStream#close()
     */
    protected void finalize() throws IOException {
        if ((fd != null) &&  (fd != FileDescriptor.in)) {
            /* if fd is shared, the references in FileDescriptor
             * will ensure that finalizer is only called when
             * safe to do so. All references using the fd have
             * become unreachable. We can call close()
             */
            close();
        }
    }
}
