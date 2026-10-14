package ie.griffith.ossim.filesystem;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A tiny hierarchical file system kept in memory, like a simplified Unix tree.
 * Paths are absolute ("/docs/a.txt") or relative to the current directory.
 * A ReadWriteLock lets many threads read at once but only one write at a time.
 */
public class InMemoryFileSystem {

    /** A file or a directory (a directory has children, a file has content). */
    private static final class Node {
        final String name;
        final Node parent;
        final boolean directory;
        final SortedMap<String, Node> children = new TreeMap<>();
        StringBuilder content = new StringBuilder();

        Node(String name, Node parent, boolean directory) {
            this.name = name;
            this.parent = parent;
            this.directory = directory;
        }
    }

    private final Node root = new Node("", null, true);
    private Node cwd = root;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void mkdir(String path) {
        write(() -> create(path, true));
    }

    public void touch(String path) {
        write(() -> create(path, false));
    }

    /** Appends text to a file, creating it if needed. */
    public void append(String path, String text) {
        write(() -> {
            Node n = find(path);
            if (n == null) n = create(path, false);
            if (n.directory) throw new FileSystemException(path + " is a directory");
            n.content.append(text);
            return null;
        });
    }

    public String read(String path) {
        return readLocked(() -> {
            Node n = require(path);
            if (n.directory) throw new FileSystemException(path + " is a directory");
            return n.content.toString();
        });
    }

    /** Lists a directory; directory names end with "/". */
    public List<String> ls(String path) {
        return readLocked(() -> {
            Node n = require(path);
            if (!n.directory) return List.of(n.name);
            List<String> out = new ArrayList<>();
            for (Node c : n.children.values()) out.add(c.directory ? c.name + "/" : c.name);
            return out;
        });
    }

    public void cd(String path) {
        write(() -> {
            Node n = require(path);
            if (!n.directory) throw new FileSystemException(path + " is not a directory");
            cwd = n;
            return null;
        });
    }

    /** Removes a file or an EMPTY directory. */
    public void rm(String path) {
        write(() -> {
            Node n = require(path);
            if (n == root) throw new FileSystemException("cannot remove /");
            if (n.directory && !n.children.isEmpty()) throw new FileSystemException(path + " is not empty");
            n.parent.children.remove(n.name);
            return null;
        });
    }

    public String pwd() {
        return readLocked(() -> pathOf(cwd));
    }

    public boolean exists(String path) {
        return readLocked(() -> find(path) != null);
    }

    /** Runs a shell-style command such as "mkdir docs" or "write a.txt hello". Used by the UI. */
    public String execute(String commandLine) {
        String[] parts = commandLine.trim().split("\\s+", 3);
        String arg = parts.length > 1 ? parts[1] : ".";
        try {
            return switch (parts[0]) {
                case "mkdir" -> { mkdir(arg); yield ""; }
                case "touch" -> { touch(arg); yield ""; }
                case "write" -> { append(arg, parts.length > 2 ? parts[2] + "\n" : "\n"); yield ""; }
                case "cat" -> read(arg);
                case "ls" -> String.join("  ", ls(arg));
                case "cd" -> { cd(parts.length > 1 ? arg : "/"); yield ""; }
                case "rm" -> { rm(arg); yield ""; }
                case "pwd" -> pwd();
                case "help" -> "commands: mkdir, touch, write <file> <text>, cat, ls, cd, rm, pwd";
                default -> "unknown command: " + parts[0];
            };
        } catch (FileSystemException e) {
            return "error: " + e.getMessage();
        }
    }

    // ---- helpers ----

    private Node create(String path, boolean directory) {
        List<String> parts = split(path);
        if (parts.isEmpty()) throw new FileSystemException("invalid path");
        String name = parts.remove(parts.size() - 1);
        Node parent = walk(startFor(path), parts);
        if (parent == null || !parent.directory) throw new FileSystemException("no such directory for " + path);
        if (parent.children.containsKey(name)) throw new FileSystemException(path + " already exists");
        Node n = new Node(name, parent, directory);
        parent.children.put(name, n);
        return n;
    }

    private Node require(String path) {
        Node n = find(path);
        if (n == null) throw new FileSystemException("no such file or directory: " + path);
        return n;
    }

    private Node find(String path) {
        return walk(startFor(path), split(path));
    }

    private Node startFor(String path) {
        return path.startsWith("/") ? root : cwd;
    }

    private Node walk(Node from, List<String> parts) {
        Node n = from;
        for (String p : parts) {
            if (n == null || !n.directory) return null;
            if (p.equals(".")) continue;
            if (p.equals("..")) { n = n.parent == null ? root : n.parent; continue; }
            n = n.children.get(p);
        }
        return n;
    }

    private static List<String> split(String path) {
        List<String> parts = new ArrayList<>();
        for (String p : path.split("/")) if (!p.isEmpty()) parts.add(p);
        return parts;
    }

    private static String pathOf(Node n) {
        if (n.parent == null) return "/";
        String parent = pathOf(n.parent);
        return (parent.equals("/") ? "" : parent) + "/" + n.name;
    }

    private <T> T write(java.util.function.Supplier<T> action) {
        lock.writeLock().lock();
        try {
            return action.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private <T> T readLocked(java.util.function.Supplier<T> action) {
        lock.readLock().lock();
        try {
            return action.get();
        } finally {
            lock.readLock().unlock();
        }
    }
}
