<?php
$firm = ("{$_REQUEST['firm']}");
$id = ("{$_REQUEST['id']}");
$dates = "{$_REQUEST['dates']}";
if ($firm) { 
	//$database = "192.168.".$firm.".10:C:/Program Files/OnlineServerPro/Database/Database.gdb";
	$database = "192.168.1.".$firm.":C:/Program Files/OnlineServerPro/Database/Database.gdb";
	$username = "SYSDBA";
	$password = "masterkey";
	$charset = "win1251";
	$db = ibase_connect($database, $username, $password, $charset);
	$stupd = "UPDATE INVENT SET VALID = '2' WHERE ID = $id ";
	$resultview = ibase_query($stupd, $db);
	while ($rows = ibase_fetch_assoc($resultview)){
		$rows .= $rows; 
	}
	ibase_close($db);
}
header('Location: invview.php?firm='.$firm.'&dates='.$dates.'&names=&mx=');
exit;
?>