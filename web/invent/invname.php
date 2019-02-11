<?php
$namecode = "{$_REQUEST['namecode']}";
$firmcode = "{$_REQUEST['firmcode']}";if ($firmcode) {
	//$database = "192.168.".$firmcode.".10:C:/Program Files/OnlineServerPro/Database/Database.gdb";
	$database = "192.168.1.".$firmcode.":C:/Program Files/OnlineServerPro/Database/Database.gdb";
	$username = "SYSDBA";
	$password = "masterkey";
	$charset = "win1251";
	$db = ibase_connect($database, $username, $password, $charset);
		if (!$config['name']) {
			$stn = "SELECT SELLERS.SLR_NAME as name FROM SELLERS WHERE SELLERS.SLR_CODE = '".$namecode."'";
			$resultstn = ibase_query($stn, $db);
			while ($rowstn = ibase_fetch_assoc($resultstn)){
				$config['name'] = iconv('windows-1251','UTF-8',$rowstn['NAME']);
			}
		}
	ibase_close($db);
}
if (!$config['firm']) $config['firm'] = $firmcode;
if (!$config['name']) $config['name'] = $namecode;
if ($config) $json = '{"cfg":[{"firm":'.$config['firm'].'},{"name":"'.$config['name'].'"}]}';
header("Content-Type: text/html; charset=utf-8");
echo $json;
?>