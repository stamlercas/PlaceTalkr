<?php
    require_once __DIR__ . '/db.php';
    
    function placeExists($placeID)
    {
        return exists('places', $placeID, 'PlaceID');
    }
    
    function usernameExists($username)
    {
        return exists('users', $username, 'Username');
    }
    
    function emailExists($email)
    {
        return exists('users', $email, 'Email');
    }
    
    function exists($table, $value, $field)
    {
        $db = new DB();
        $db->bind('value', $value);
        $result = $db->query("SELECT COUNT(*) AS `Count` FROM $table WHERE $field = :value");
        if ($result[0]['Count'] == 0)
            return false;
        return true;
    }

