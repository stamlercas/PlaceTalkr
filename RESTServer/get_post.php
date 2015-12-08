<?php
    require_once __DIR__ . '/db.php';
    $response = array();
    $db = new DB();
    //get post id
    if (isset($_POST['PostID']))
    {
        $postID = $_POST['PostID'];
        $db->bind('PostID', $postID);
        $result = $db->query("SELECT PostID, Content, Time, users.Username FROM posts "
                . "JOIN users ON posts.userID = users.userID WHERE PostID = :PostID ORDER BY Time DESC");
        
        $db->bind('PostID', $postID);
        $comments = $db->query("SELECT CommentID, c.Content, c.Time, u.Username "
                . "FROM comments c "
                . "JOIN users u on u.userID = c.UserID "
                . "WHERE PostID = :PostID "
                . "ORDER BY c.Time DESC");
        
        //if result actually contains something
        if (!empty($result))
        {
            //should only be one row
            if (!(count($result) == 0))
            {
                $response['success'] = 1;
                $response['message'] = 'Post successfully grabbed.';
                $response['post'] = array();
                $post = array();
                $post['PostID'] = $result[0]['PostID'];
                $post['Content'] = $result[0]['Content'];
                $post['Time'] = date('n/d/Y g:i a', strtotime($result[0]['Time']));
                $post['Username'] = $result[0]['Username'];
                
                
                array_push($response['post'], $post);
                
                $response['comments'] = array();
                foreach($comments as $row)
                {
                    $comment = array();
                    
                    $comment['CommentID'] = $row['CommentID'];
                    $comment['Content'] = $row['Content'];
                    $comment['Time'] = date('n/d/Y g:i a', strtotime($row['Time']));
                    $comment['Username'] = $row['Username'];
                    
                    array_push($response['comments'], $comment);
                }
                
                if (count($comments) == 0)
                {
                    $response['FirstToComment'] = 1;
                }
                else
                {
                    $response['FirstToComment'] = 0;
                }
                
                echo json_encode($response);
            }
            else
            {
                //not found
                $response['success'] = 0;
                $response['message'] = 'No post found.';
                echo json_encode($response);
            }
        }
        else
        {
            $response['success'] = 0;
            $response['message'] = 'No post found.';
            echo json_encode($response);
        }
    }
    else
    {
        //missing id
        $response['success'] = 0;
        $response['message'] = 'Required field(s) is missing.';
        echo json_encode($response);
    }
?>
