package main.java.jonathan.blocnotas;

import java.io.InputStream;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import main.java.jonathan.blocnotas.util.Navegador;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Bloc de notas");
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        
        // Icono de la ventana
        try (InputStream is = MainApp.class.getResourceAsStream("/main/resources/img/icono.ico")) {
            if (is != null) {
                stage.getIcons().add(new Image(is));
            }
        } catch (Exception ignorado) {
            // si falla, se usa el icono por defecto de Java
        }


        Navegador.iniciar(stage);
        Navegador.irAInicio();

        stage.setOnCloseRequest(e -> Navegador.guardarAlSalir());
        stage.show();
    }

    @Override
    public void stop() {
        Navegador.guardarAlSalir();
    }

    public static void main(String[] args) {
        launch(args);
    }
}