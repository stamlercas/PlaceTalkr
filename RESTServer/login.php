<?php
    require_once __DIR__ . '/db.php';
    
    $response = array();
    
    if (isset($_POST['Username']) && isset($_POST['Password']) || true)
    {
        $db = new DB();
		$username = $_POST["Username"];
		$password = $_POST['Password'];
        $db->bind('Username', $username);
        $db->bind('Password', $password);
        
        $result = $db->query("SELECT UserID, FirstName, LastName, Username, Password, Email FROM users "
                . "WHERE Username = :Username AND Password = :Password");
        
        if ($result)
        {
            
            $response['success'] = 1;
            $response['user'] = array();
            array_push($response['user'], $result);
            echo json_encode($response);
        }
        else
        {
            $response['success'] = 0;
            $response['message'] = "An error has occurred.";
            echo json_encode($response);
        }
    }
    else
    {
        $response['success'] = 0;
        $resposne['message'] = 'Required field(s) are missing.';
        echo json_encode($response);
    }

?>
