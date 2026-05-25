-- Creates the application database

CREATE DATABASE mesos CHARACTER SET utf8mb4;
CREATE USER 'mesos'@'localhost' IDENTIFIED BY 'mesos';
GRANT ALL PRIVILEGES ON mesos.* TO 'mesos'@'localhost';
FLUSH PRIVILEGES;