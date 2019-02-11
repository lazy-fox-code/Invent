<?php 
$firmcode = "{$_REQUEST['firmcode']}";
$barcode = "{$_REQUEST['barcode']}";
$mxcode = "{$_REQUEST['mxcode']}";
$roles = "{$_REQUEST['roles']}";

if (strlen($barcode)==13) $barcode = substr($barcode, 0, 12);
if ($firmcode) {
	$database = "192.168.1.".$firmcode.":C:/Program Files/OnlineServerPro/Database/Database.gdb";
	$username = "SYSDBA";
	$password = "masterkey";
	$charset = "win1251";
	$db = ibase_connect($database, $username, $password, $charset);
       	if ($firmcode & $barcode) {
			$stp = "SELECT FIRST 1
					    B.BC_PRODUCT_ID as PRODID,
					    P.PROD_NAME as PRODNAME,
					    B.BC_CFC as CFC
					FROM BARCODES B, PRODUCTS P
					   INNER JOIN BARCODES ON (P.PROD_PRODUCT_ID = B.BC_PRODUCT_ID)
					WHERE (B.BC_BARCODE = ".$barcode.")";
			$resultstp = ibase_query($stp, $db);
			while ($rowstp = ibase_fetch_assoc($resultstp)){
				@$zapros['data']['prodid'] = $rowstp['PRODID'];
				@$zapros['data']['prodname'] = iconv('windows-1251','UTF-8',$rowstp['PRODNAME']);
				@$zapros['data']['cfc'] = (0+$rowstp['CFC']);
			}
		}

		if ($zapros) {
			$stcfc = "SELECT DISTINCT B.BC_CFC as CFC FROM BARCODES B WHERE (B.BC_PRODUCT_ID = ".$zapros['data']['prodid'].")";
			@$resultstcfc = ibase_query($stcfc, $db);
			while ($rowstcfc = ibase_fetch_assoc($resultstcfc)){
				$mas .= (0+$rowstcfc['CFC']).',';
			}
            $mas = substr($mas, 0, -1);
		}
		//если из шкафа продали после подсчёта не писать
		if ($roles == 'inv') {
			$str = "SELECT INVENT.ROLES AS ROL FROM INVENT WHERE ((INVENT.MX = ".$mxcode.") AND (INVENT.PRODCODE = ".$zapros['data']['prodid'].") AND (INVENT.VALID = 1))";
			$resultstr = ibase_query($str, $db);
			while ($rowstr = ibase_fetch_assoc($resultstr)){
				$er .= $rowstr['ROL'];
			}
			$err = strpos($er, 'vk'); //invkas	
		}		
	ibase_close($db);
}
header("Content-Type: text/html; charset=utf-8");
if (!$zapros) echo '{"zapros":[{"prodid":"0"},{"tovar":"null"},{"cfc":"0"},{"cfcst":"0"}]}';
if ($err) echo '{"zapros":[{"prodid":"0"},{"tovar":"Товар ранее был записан"},{"cfc":"0"},{"cfcst":"0"}]}';
if ($err === false || $roles == 'kas') echo '{"zapros":[{"prodid":"'.$zapros['data']['prodid'].'"},{"tovar":"'.$zapros['data']['prodname'].'"},{"cfc":"'.$zapros['data']['cfc'].'"},{"cfcst":"'.$mas.'"}]}';
?>