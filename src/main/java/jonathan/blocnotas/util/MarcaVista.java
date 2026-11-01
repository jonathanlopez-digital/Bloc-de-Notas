package main.java.jonathan.blocnotas.util;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import main.java.jonathan.blocnotas.model.Marca;

public final class MarcaVista {

    private MarcaVista() {}

    public static Circle punto(Marca m) {
        Circle c = new Circle(6);
        if (m.esNinguna()) {
            c.setFill(Color.TRANSPARENT);
            c.setStroke(Color.GRAY);
        } else {
            c.setFill(Color.web(m.getColor()));
        }
        return c;
    }
}