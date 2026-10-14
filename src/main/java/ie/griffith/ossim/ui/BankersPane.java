package ie.griffith.ossim.ui;

import ie.griffith.ossim.deadlock.BankersAlgorithm;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Arrays;
import java.util.List;

/** Edit the Banker's matrices as text, check safety and try resource requests. */
public class BankersPane extends BorderPane {
    private final TextField available = new TextField("3 3 2");
    private final TextArea max = new TextArea("7 5 3\n3 2 2\n9 0 2\n2 2 2\n4 3 3");
    private final TextArea allocation = new TextArea("0 1 0\n2 0 0\n3 0 2\n2 1 1\n0 0 2");
    private final TextField requestProcess = new TextField("1");
    private final TextField request = new TextField("1 0 2");
    private final TextArea output = new TextArea();

    public BankersPane() {
        output.setEditable(false);
        output.setStyle("-fx-font-family: monospace");
        max.setPrefRowCount(5);
        allocation.setPrefRowCount(5);
        Button check = new Button("Check safety");
        Button req = new Button("Request");
        check.setOnAction(e -> check());
        req.setOnAction(e -> doRequest());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(6);
        form.setPadding(new Insets(10));
        form.addRow(0, new Label("Available:"), available);
        form.addRow(1, new Label("Max (one row per process):"), max, new Label("Allocation:"), allocation);
        form.addRow(2, check, new Label("Process index:"), requestProcess);
        form.addRow(3, new Label("Request vector:"), request, req);
        setTop(form);
        setCenter(output);
    }

    private BankersAlgorithm build() {
        return new BankersAlgorithm(vector(available.getText()), matrix(max.getText()), matrix(allocation.getText()));
    }

    private void check() {
        try {
            BankersAlgorithm b = build();
            List<Integer> seq = b.safeSequence();
            StringBuilder sb = new StringBuilder("Need matrix:\n");
            for (int[] row : b.need()) sb.append(Arrays.toString(row)).append('\n');
            sb.append(seq.isEmpty() ? "\nUNSAFE - deadlock is possible\n"
                    : "\nSAFE. Safe sequence: " + seq.stream().map(i -> "P" + i).toList() + "\n");
            output.setText(sb.toString());
        } catch (RuntimeException e) {
            output.setText("Input error: " + e.getMessage());
        }
    }

    private void doRequest() {
        try {
            BankersAlgorithm b = build();
            int p = Integer.parseInt(requestProcess.getText().trim());
            boolean granted = b.request(p, vector(request.getText()));
            output.setText(granted
                    ? "Request GRANTED. New available: " + Arrays.toString(b.available()) + "\nSafe sequence: " + b.safeSequence()
                    : "Request DENIED - P" + p + " must wait (not enough resources or would be unsafe)");
        } catch (RuntimeException e) {
            output.setText("Error: " + e.getMessage());
        }
    }

    private static int[] vector(String text) {
        return Arrays.stream(text.trim().split("\\s+")).mapToInt(Integer::parseInt).toArray();
    }

    private static int[][] matrix(String text) {
        return Arrays.stream(text.trim().split("\n")).map(BankersPane::vector).toArray(int[][]::new);
    }
}
