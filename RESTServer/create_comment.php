<?php
    require_once __DIR__ . '/db.php';

    //response for JSON
    $response = array();
    
    if (isset($_POST['Content']) && isset($_POST['UserID']) && isset($_POST['PostID']) || true)
    {
        $content = $_POST['Content'];
        $userID = $_POST['UserID'];
        $postID = $_POST['PostID'];
        
        $db = new DB();
        
        $db->bind('Content', $content);
        $db->bind('UserID', $userID);
        $db->bind('PostID', $postID);
        
        $result = $db->query("INSERT INTO comments(Content, UserID, PostID) VALUES (:Content, :UserID, :PostID)");
        
        if ($result)
        {
            //successfully inserted into db
            $response['success'] = 1;
            $response['message'] = 'Comment successfully created';
            $response['PostID'] = $postID;
            
            //JSON response
            echo json_encode($response);
        }
        else
        {
            //failed to insert
            $response['success'] = 0;
            $response['message'] = 'An error occurred.';
            
            //JSON response
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