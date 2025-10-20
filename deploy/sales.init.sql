-- MySQL dump 10.13  Distrib 9.3.0, for Linux (x86_64)
--
-- Host: localhost    Database: RPSalesDB
-- ------------------------------------------------------
-- Server version	9.3.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `sales_details`
--
CREATE DATABASE IF NOT EXISTS RPSalesDB;

USE RPSalesDB;

DROP TABLE IF EXISTS `sales_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sales_details` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `sales_price_per_unit` decimal(38,2) NOT NULL,
  `sale_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmofh1590bbm744ll9r3aywtk` (`sale_id`),
  CONSTRAINT `FKmofh1590bbm744ll9r3aywtk` FOREIGN KEY (`sale_id`) REFERENCES `sales_transaction` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sales_details`
--

LOCK TABLES `sales_details` WRITE;
/*!40000 ALTER TABLE `sales_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `sales_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sales_tax`
--

DROP TABLE IF EXISTS `sales_tax`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sales_tax` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tax_rate` decimal(38,2) DEFAULT NULL,
  `tax_type` enum('GST') DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sales_tax`
--

LOCK TABLES `sales_tax` WRITE;
/*!40000 ALTER TABLE `sales_tax` DISABLE KEYS */;
INSERT INTO `sales_tax` (`id`, `tax_rate`, `tax_type`) VALUES (1, 0.09, 'GST');
/*!40000 ALTER TABLE `sales_tax` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sales_transaction`
--

DROP TABLE IF EXISTS `sales_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sales_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_entity_id` bigint NOT NULL,
  `sales_tax_amount` decimal(38,2) DEFAULT NULL,
  `subtotal` decimal(38,2) DEFAULT NULL,
  `total` decimal(38,2) DEFAULT NULL,
  `transaction_date` datetime(6) NOT NULL,
  `sales_tax_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7wq54e1ubjodfnaph9fu12grq` (`sales_tax_id`),
  CONSTRAINT `FK7wq54e1ubjodfnaph9fu12grq` FOREIGN KEY (`sales_tax_id`) REFERENCES `sales_tax` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sales_transaction`
--

LOCK TABLES `sales_transaction` WRITE;
/*!40000 ALTER TABLE `sales_transaction` DISABLE KEYS */;
/*!40000 ALTER TABLE `sales_transaction` ENABLE KEYS */;
UNLOCK TABLES;

