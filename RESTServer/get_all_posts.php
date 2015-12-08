<?php
    require_once __DIR__ . '/db.php';
    //array for json response
    $response = array();
    
    if (isset($_POST['PlaceID']) && isset($_POST['Name']) && isset($_POST['Longitude']) && isset($_POST['Latitude']))
    {
        $placeID = $_POST['PlaceID'];
        $name = $_POST['Name'];
        $longitude = $_POST['Longitude'];
        $latitude = $_POST['Latitude'];
        
        require_once 'ifExists.php';
        $db = new DB();
        //if place doesn't exist, make it exist
        if (!placeExists($placeID))
        {
            $db->bind('PlaceID', $placeID);
            $db->bind('Name', $name);
            $db->bind('Longitude', $longitude);
            $db->bind('Latitude', $latitude);
            $db->query('INSERT INTO `places` (PlaceID, Name, Longitude, Latitude) '
                    . 'VALUES(:PlaceID, :Name, :Longitude, :Latitude)');
        }
        
        $db->bind('PlaceID', $placeID);
        $results = $db->query("SELECT PostID, Content, Time, posts.UserID, users.Username, places.Name AS Name "
                . "FROM posts "
                . "JOIN users ON posts.UserID = users.UserID "
                . "JOIN places ON places.PlaceID = posts.PlaceID "
                . "WHERE posts.PlaceID = :PlaceID "
                . "ORDER BY Time DESC");
        
        $db->bind('PlaceID', $placeID);
        $name = $db->query("SELECT Name FROM places WHERE PlaceID = :PlaceID");
        
        //results is not empty
        if (count($results) != 0)
        {
            //successful
            $response['success'] = 1;
            $response['message'] = "Posts successfully grabbed.";
            $response['PlaceName'] = $name[0]['Name'];
            $response['FirstToPost'] = 0;

            //putting results into json object
            $response['posts'] = array();
            foreach($results as $row)
            {
                $post = array();
                $post['PostID'] = $row['PostID'];
                $post['Content'] = $row['Content'];

                $time = date('n/d/Y g:i a', strtotime($row['Time']));
                $post['Time'] = $time;
                $post['UserID'] = $row['UserID'];
                $post['Username'] = $row['Username'];
                array_push($response['posts'], $post);
            }

            //return json
            echo json_encode($response);
        }
        else
        {
            //posts are empty
            $response['success'] = 1;
            $response['message'] = 'No posts have been made.';
            $response['PlaceName'] = $name[0]['Name'];
            $response['FirstToPost'] = 1;       //no posts have been made
            echo json_encode($response);
        }
    }
    else
    {
        //error
        $response['success'] = 0;
        $response['message'] = 'Required field(s) are missing.';
        echo json_encode($response);
    }
?>

