module org.example.hometracker_kurs {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires telegrambots;
    requires telegrambots.meta;
    requires org.junit.jupiter.api;


    exports org.example.hometracker_kurs.controller;
    opens org.example.hometracker_kurs.controller to javafx.fxml;
    exports org.example.hometracker_kurs.model;
    opens org.example.hometracker_kurs.model to javafx.fxml;
    exports org.example.hometracker_kurs.view;
    opens org.example.hometracker_kurs.view to javafx.fxml;
}