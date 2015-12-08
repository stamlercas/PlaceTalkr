<?php
class Database
{   
    private $host;
    private $dbname;
    private $username;
    private $password;
    
    private $log;
    
    public function __construct()
    {
        require_once __DIR__ . '/db_config.php';
        $this->host = HOST;
        $this->dbname = DB_NAME;
        $this->username = DB_USER;
        $this->password = DB_PASSWORD;

        //$this->log = new Log();

        $this->db = getDBConnection();
        return $this->db;
    }
    
    //connects db
    private function getDBConnection()
    {
        $dsn = "mysql:host=$host;dbname=$dbname";

        try
        {
            $db = new PDO($dsn, $this->username, $this->password);
        } catch (PDOException $e)
        {
            //write into log
            //echo $this->ExceptionLog($e->getMessage());
            die;
        }
        return $db;
    }
}
