<?php
$firm = ("{$_REQUEST['firm']}");
$mx = "{$_REQUEST['mx']}";
$prodcode = ("{$_REQUEST['prodcode']}");
$roles = "inv";
$valid = 1;
if ($firm) { 
	//$database = "192.168.".$firm.".10:C:/Program Files/OnlineServerPro/Database/Database.gdb";
	$database = "192.168.1.".$firm.":C:/Program Files/OnlineServerPro/Database/Database.gdb";
	$username = "SYSDBA";
	$password = "masterkey";
	$charset = "win1251";
	$db = ibase_connect($database, $username, $password, $charset);
	$sterrupd = "UPDATE INVENT SET VALID = '2' 
	WHERE (	(INVENT.PRODCODE = ".$prodcode.")
		AND (FIRM = ".$firm.")
		AND (MX = ".$mx.")
		AND (DATES = 'NOW')
		AND (ROLES = '".$roles."')
		AND (valid = ".$valid.")
		)";
	$resulterr = ibase_query($sterrupd, $db);
	ibase_close($db);
}
if ($resulterr) $json = '{"resulterr":[{"del":"'.$resulterr.'"}]}';
else $json = '{"resulterr":[{"del":"null"}]}';
header("Content-Type: text/html; charset=utf-8");
echo $json;	
?>