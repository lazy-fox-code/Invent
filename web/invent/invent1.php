<?php 
$firm = ("{$_REQUEST['firm']}");
$dates = "{$_REQUEST['dates']}";
if ($dates === "") $dates = date("Y-m-d");	
if ($firm) {
	$database = "192.168.".$firm.".10:C:/Program Files/OnlineServerPro/Database/Database.gdb";
	//$database = "192.168.1.".$firm.":C:/Program Files/OnlineServerPro/Database/Database.gdb";
	$username = "SYSDBA";
	$password = "masterkey";
	$charset = "win1251";
	$db = ibase_connect($database, $username, $password, $charset);
	if ($db) {$stviewpart = "
		SELECT 
			PRODUCTS.PROD_NAME AS PRODNAME,
			SUM(PART.PART_REST) AS PARTREST
		FROM PRODUCTS
		   INNER JOIN PART ON (PRODUCTS.PROD_PRODUCT_ID = PART.PART_PRODUCT_ID)
		GROUP BY PRODUCTS.PROD_NAME";	
		$resultviewpart = ibase_query($stviewpart, $db);
		while ($rowspart = ibase_fetch_assoc($resultviewpart)){
			$resultpart[$rowspart['PRODNAME']]['PARTREST'] = (0+$rowspart['PARTREST']);
		}
	}
	if ($db) {$stview = "
		SELECT 
			PRODUCTS.PROD_NAME AS PRODNAME,
			SUM(INVENT.CFC*INVENT.QTY) AS QTY,
			MAX(PRODUCTS.PROD_REST) AS REST,
			SUM(PRODUCTS.PROD_SALED) AS SALED
		FROM PRODUCTS
		   LEFT OUTER JOIN INVENT ON (PRODUCTS.PROD_PRODUCT_ID = INVENT.PRODCODE)
		   WHERE (
		   ((PRODUCTS.PROD_REST >0) OR (PRODUCTS.PROD_SALED>0) OR (INVENT.QTY>0))
		   AND ((INVENT.VALID = 1) or (INVENT.VALID is null))
		   AND ((INVENT.DATES = '$dates') or (INVENT.DATES is null))
			  )
			  GROUP BY PRODUCTS.PROD_NAME";	
			  
		$resultview = ibase_query($stview, $db);
		while ($rows = ibase_fetch_assoc($resultview)){
			$result[$rows['PRODNAME']]['QTY'] = (0+$rows['QTY']);
			$result[$rows['PRODNAME']]['REST'] = (0+$rows['REST']);
			$result[$rows['PRODNAME']]['SALED'] = (0+$rows['SALED']);			
		}		
	ibase_close($db);
	}
	$result = array_merge_recursive ($result,$resultpart);
var_dump($result); 	
//Таблицу заполним учётными данными по продажам/остаткам и фактическими данными по продажам/остаткам
}
?>
<html>
<head>
	<title>Справочная служба - Журнал инвентаризации товара</title>
	<meta http-equiv="Content-Type" content="text/html; charset=windows-1251">
	<link rel="STYLESHEET" type="text/css" href="mainpage.css">
	<script src="sorttable.js" type="text/javascript"></script>
	<style type="text/css">
<!-- 		body{font-family: Arial; font-size:10px; color:#555555;}
		div.main{margin:1px; width:800px;}
		table.sortable{border:0; padding:0; margin:0;}
		table.sortable td{padding:1px; width:400px; border-bottom:solid 1px #DEDEDE;}
		table.sortable th{padding:4px;}
		table.sortable thead{background:#e3edef; color:#333333; text-align:left;}
		table.sortable tfoot{font-weight:bold; }
		table.sortable tfoot td{border:none;} -->
		A 	{
			text-decoration: none; // Убирает подчеркивание для ссылок 
			color: blue; // Ссылка синего цвета 
		}
		A:visited { text-decoration: none; }
		A:hover {
			text-decoration: underline; // Добавляем подчеркивание при наведении курсора на ссылку 
			color: blue; // Ссылка синего цвета 
		}
	</style>
</head>
<!-- <body bgcolor="#FFFFFF" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
<div class=subheader>&nbsp;</div> 
<div class="main">-->
<?php
//Далее выводим итоговую таблицу:
echo "<table class = 'sortable' border='1' cellpadding='5'>";
	echo "<thead>";
		echo "<th style='border:1px solid black;' ><font size='-1'>Наименование_фактически_записанного_операторами_товара_в_ведомость</font></th>";
		echo "<th style='border:1px solid black;' ><font size='-1'>ОстФ</font></th>";
		echo "<th style='border:1px solid black;' ><font size='-1'>ОстУ</font></th>";
		echo "<th style='border:1px solid black;' ><font size='-1'>ПрУ</font></th>";
	echo "</thead></form>";
	echo "<tbody>";
		foreach ($result as $key => $row) {
			echo "<tr>";
				echo "<td>".'<a href="invview.php?firm='.$firm.'&dates='.$dates.'&prodname='.$key.'&mx=" title="Развернуть">'.$key.'</a>'."</td>";	 						
				echo "<td>".$result[$key]['QTY']."</td>"; 
				if ($result[$key]['PARTREST']) echo "<td>".($result[$key]['REST']+$result[$key]['PARTREST'])."</td>";
				else echo "<td>".$result[$key]['REST']."</td>";
				echo "<td>".$result[$key]['SALED']."</td>";
			echo "</tr>";
		}			
	echo "</tbody>";
echo "</table>";
echo "</div>";
?>
</body>
</html>