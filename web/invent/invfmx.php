<?php
$mxcode = "{$_REQUEST['mxcode']}";
$firmcode = "{$_REQUEST['firmcode']}";
$dates = date("Y-m-d");if ($firmcode) {
	//$database = "192.168.".$firmcode.".10:C:/Program Files/OnlineServerPro/Database/Database.gdb";
	$database = "192.168.1.".$firmcode.":C:/Program Files/OnlineServerPro/Database/Database.gdb";
	$username = "SYSDBA";
	$password = "masterkey";
	$charset = "win1251";
	$db = ibase_connect($database, $username, $password, $charset);
        if (!$config['firm']) {
			$stf = "SELECT CONFIG.CFG_VALUE as firm FROM CONFIG WHERE CONFIG.CFG_PARAM = 'CODEFIRM'";
			$resultstf = ibase_query($stf, $db);
			while ($rowstf = ibase_fetch_assoc($resultstf)){
				$config['firm'] = iconv('windows-1251','UTF-8',$rowstf['FIRM']);
			}
		}
		if (!$config['mx']) {			$tmx = substr($mxcode, 0, 1);
			$tmxarr = Array ("В","Ш","П","Х","C");			$config['mx'] = $tmxarr[$tmx-1].substr($mxcode, 1, 2);
		}
		$stmxlist = "SELECT PRODUCTS.PROD_NAME AS PN, INVENT.PRODCODE AS PC, (INVENT.CFC * INVENT.QTY) AS QT 
		FROM INVENT INNER JOIN PRODUCTS ON (INVENT.PRODCODE = PRODUCTS.PROD_PRODUCT_ID) 
		WHERE ( (INVENT.FIRM = $firmcode) 
			AND (INVENT.MX = $mxcode) 
			AND (INVENT.ROLES = 'inv') 
			AND (INVENT.VALID = '1') 
			AND (INVENT.DATES = '$dates')
		)";
		$resultmxlist = ibase_query($stmxlist, $db);
			while ($rowmxlist = ibase_fetch_assoc($resultmxlist)){
				$mxlist .= '{"mpid":"'.($rowmxlist['PC']+0).'","mtov":"'.iconv('windows-1251','UTF-8',$rowmxlist['PN']).'","mqty":"'.($rowmxlist['QT']+0).'"},';
			}
			$mxlist = substr($mxlist, 0, -1);
	ibase_close($db);
}
if (!$config['firm']) $config['firm'] = $firmcode;
if (!$config['mx']) $config['mx'] = $mxcode;
if ($config) $json = '{"cfg":[{"firm":"'.$config['firm'].'"},{"mx":"'.$config['mx'].'"}],"mxlist":['.$mxlist.']}';
header("Content-Type: text/html; charset=utf-8");
echo $json;
?>