<?php
$firm = "{$_REQUEST['firm']}";
$mx = "{$_REQUEST['mx']}";
$seller = "{$_REQUEST['seller']}";
$roles = "{$_REQUEST['roles']}";
$barcode = "{$_REQUEST['barcode']}";
$prodcode = "{$_REQUEST['prodcode']}";
$cfc = "{$_REQUEST['cfc']}";
$qty = "{$_REQUEST['qty']}";
$valid = 1;
if ($roles == 'kas') $qty = $qty * - 1;
if ($firm) {
		//$database = "192.168.".$firm.".10:C:/Program Files/OnlineServerPro/Database/Database.gdb";
		$database = "192.168.1.".$firm.":C:/Program Files/OnlineServerPro/Database/Database.gdb";
		$username = "SYSDBA";
		$password = "masterkey";
		$charset = "win1251";
	if ($prodcode == 0) {}
		else { 
		$db = ibase_connect($database, $username, $password, $charset);
		if ($db) {
			$stpn = "
				SELECT PRODUCTS.PROD_NAME AS PRODNAME
				FROM PRODUCTS
				WHERE (PRODUCTS.PROD_PRODUCT_ID = ".$prodcode.")";
			$respn = ibase_query($stpn, $db);
			while ($rpn = ibase_fetch_assoc($respn)){
				$prodname = ($rpn['PRODNAME']);		
			}
			if ($roles == "kas"){
				$stvalid = " 
					SELECT SUM(INVENT.VALID) AS SUM_VALID
					FROM INVENT
					WHERE ( (INVENT.DATES = 'NOW')
						AND	(INVENT.FIRM = ".$firm.")
						AND (INVENT.MX = ".$mx.")
						AND (INVENT.PRODCODE = ".$prodcode.")
						AND (INVENT.ROLES = 'inv')
					)";
				$res = ibase_query($stvalid, $db);
				while ($rws = ibase_fetch_assoc($res)){
					$vld = ($rws['SUM_VALID']);		
				}
				if ($vld == null) $valid = 3;
				else $valid = 1;
			}	
			if ($valid) {
				$stsave = "INSERT INTO INVENT (ID, DATES, TIMES, FIRM, MX, SELLER, ROLES, BARCODE, PRODCODE, CFC, QTY, VALID, PRODNAME)
				VALUES (GEN_ID(GEN_INVENT_ID,1), 'NOW', 'NOW', '".$firm."', '".$mx."', '".$seller."', '".$roles."', ".$barcode.", ".$prodcode.", ".$cfc.", ".$qty.", ".$valid.", '".$prodname."')";
				$resultsave = ibase_query($stsave, $db);
			}
		ibase_close($db);
		}
	}
}
if ($resultsave) $json = '{"resultsave":[{"add":"'.$resultsave.'"}]}';
else $json = '{"resultsave":[{"add":"null"}]}';
header("Content-Type: text/html; charset=utf-8");
echo $json;
?>