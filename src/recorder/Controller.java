package recorder;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class Controller implements Initializable {

    @FXML Text progress;
    @FXML TextField link, filename;
    @FXML ComboBox qualities;
    @FXML Button save, cancel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        link.textProperty().addListener((ObservableValue<? extends String> o, String oldValue, String newValue) -> {
            qualities.getItems().clear();

            new Thread(() -> {
                try {
                    qualities.getItems().addAll(getQualityAlternatives(link.getText()));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Optional<String> first = qualities.getItems().stream().findFirst();
                if (first.isPresent())
                    Platform.runLater(() -> {
                        qualities.setValue(first.get());
                        filename.setDisable(false);
                        save.setDisable(false);
                    });
                else
                    Platform.runLater(() -> {
                        filename.clear();
                        filename.setDisable(true);
                        save.setDisable(true);
                    });
            }).start();

        });
    }

    @FXML protected void handleSaveButton() {
        new Thread(() -> {
            try {
                saveVideo(link.getText(), qualities.getValue().toString(), filename.getText());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML protected void handleCancel () {

    }

    private List<String> getQualityAlternatives (String url) throws IOException, InterruptedException {
        List<String> ret = new ArrayList<>();
        ProcessBuilder processBuilder = new ProcessBuilder("livestreamer", url);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            if(line.contains("Available streams:")) {
                StringTokenizer st = new StringTokenizer(line, ",");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    token = token.substring(token.indexOf(":")+1, token.length()).trim();
                    if(token.contains("("))
                        token = token.substring(0, token.indexOf("(")).trim();
                    ret.add(token);
                }
            }
        }

        process.waitFor();
        return ret;
    }

    private void saveVideo (String url, String quality, String filename) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("livestreamer", url, quality, "-o", filename);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        Platform.runLater(() -> cancel.setVisible(false)); //Fix cancel function
        while ((line = reader.readLine()) != null) {
            final String l = line;
            Platform.runLater(() -> progress.setText(l));
        }

        process.waitFor();
        convertVideo(filename);
    }

    private void convertVideo (String filename) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-i", filename, "-c:v", "copy", "-strict", "-2", filename + ".mp4");
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            final String l = line;
            Platform.runLater(() -> progress.setText(l));
        }

        process.waitFor();
        Platform.runLater(() -> cancel.setVisible(false));
        removeStream(filename);
    }

    private void removeStream (String filename) {
        new File(filename).delete();
    }
}

// http://www.svtplay.se/video/7872057/true-detective/true-detective-sasong-2-avsnitt-6
