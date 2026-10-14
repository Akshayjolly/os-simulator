package ie.griffith.ossim;

import ie.griffith.ossim.filesystem.FileSystemException;
import ie.griffith.ossim.filesystem.InMemoryFileSystem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemTest {
    @Test
    void createWriteReadAndList() {
        InMemoryFileSystem fs = new InMemoryFileSystem();
        fs.mkdir("/docs");
        fs.append("/docs/notes.txt", "hello ");
        fs.append("/docs/notes.txt", "world");
        assertEquals("hello world", fs.read("/docs/notes.txt"));
        assertEquals(List.of("docs/"), fs.ls("/"));
        assertEquals(List.of("notes.txt"), fs.ls("/docs"));
    }

    @Test
    void relativePathsAndCd() {
        InMemoryFileSystem fs = new InMemoryFileSystem();
        fs.mkdir("a");
        fs.cd("a");
        fs.mkdir("b");
        assertEquals("/a", fs.pwd());
        fs.cd("b");
        assertEquals("/a/b", fs.pwd());
        fs.cd("../..");
        assertEquals("/", fs.pwd());
        assertTrue(fs.exists("/a/b"));
    }

    @Test
    void errors() {
        InMemoryFileSystem fs = new InMemoryFileSystem();
        fs.mkdir("/d");
        fs.touch("/d/f");
        assertThrows(FileSystemException.class, () -> fs.mkdir("/d"));
        assertThrows(FileSystemException.class, () -> fs.rm("/d"));      // not empty
        assertThrows(FileSystemException.class, () -> fs.read("/missing"));
        assertThrows(FileSystemException.class, () -> fs.touch("/nope/f"));
        fs.rm("/d/f");
        fs.rm("/d");
        assertFalse(fs.exists("/d"));
    }

    @Test
    void shellCommands() {
        InMemoryFileSystem fs = new InMemoryFileSystem();
        fs.execute("mkdir home");
        fs.execute("write home/a.txt hi there");
        assertEquals("hi there\n", fs.execute("cat home/a.txt"));
        assertTrue(fs.execute("cat nothing").startsWith("error"));
    }

    @Test
    void concurrentAppendsAreNotLost() throws InterruptedException {
        InMemoryFileSystem fs = new InMemoryFileSystem();
        fs.touch("/log");
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            Thread th = new Thread(() -> { for (int i = 0; i < 250; i++) fs.append("/log", "x"); });
            threads.add(th);
            th.start();
        }
        for (Thread th : threads) th.join();
        assertEquals(2000, fs.read("/log").length());
    }
}
