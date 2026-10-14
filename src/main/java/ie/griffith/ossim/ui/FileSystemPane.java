package ie.griffith.ossim.ui;

import ie.griffith.ossim.filesystem.InMemoryFileSystem;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** A tiny terminal for the in-memory file system. Type "help" for commands. */
public class FileSystemPane extends BorderPane {
    private final InMemoryFileSystem fs = new InMemoryFileSystem();
    private final TextArea console = new TextArea("Type 'help' for commands.\n");
    private final TextField command = new TextField();
    private final Label prompt = new Label("/ $");

    public FileSystemPane() {
        console.setEditable(false);
        console.setStyle("-fx-font-family: monospace");
        command.setOnAction(e -> {
            String line = command.getText();
            command.clear();
            if (line.isBlank()) return;
            String result = fs.execute(line);
            console.appendText(prompt.getText() + " " + line + "\n" + (result.isEmpty() ? "" : result + "\n"));
            prompt.setText(fs.pwd() + " $");
        });
        HBox bottom = new HBox(8, prompt, command);
        HBox.setHgrow(command, Priority.ALWAYS);
        bottom.setPadding(new Insets(8));
        setCenter(console);
        setBottom(bottom);
    }
}
