<?php
    require_once __DIR__ . '/db.php';
    
    //response for json
    $response = array();
    
    if (isset($_POST['FirstName']) && isset($_POST['LastName']) && isset($_POST['Username']) && isset($_POST['Password'])
            && isset($_POST['Email']))
    {
        $fname = $_POST['FirstName'];
        $lname = $_POST['LastName'];
        $username = $_POST['Username'];
        $password = $_POST['Password'];
        $email = $_POST['Email'];
        
        require_once __DIR__. '/ifExists.php';
        if (!usernameExists($username))
        {
            if (!emailExists($email))
            {
                $db = new DB();
        
                $db->bind('FirstName', $fname);
                $db->bind('LastName', $lname);
                $db->bind('Username', $username);
                $db->bind('Password', $password);
                $db->bind('Email', $email);

                $result = $db->query("INSERT INTO `users`(FirstName, LastName, Username, Password, Email) "
                        . "VALUES (:FirstName, :LastName, :Username, :Password, :Email)");
                if ($result)
                {
                    //successful
                    $response["success"] = 1;
                    $response["message"] = "User successfully registered.";

                    echo json_encode($response);
                }
                else
                {
                    $response["success"] = 0;
                    $response["message"] = 'An error occurred.';

                    echo json_encode($response);
                }
            }
            else
            {
                $response['success'] = 0;
                $response['message'] = 'Email already exists';
                
                echo json_encode($response);
            }
        }
        else 
        {
            $response['success'] = 0;
            $response['message'] = 'Username already exists';
            
            echo json_encode($response);
        }
    }
    else 
    {
        //missing fields
        $response['success'] = 0;
        $response['message'] = 'Required field(s) are missing.';
        
        echo json_encode($response);
    }
?>

