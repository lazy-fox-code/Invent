<?php 
session_start();
$fdates[] .= '';
if ($_SESSION['config']['fdates']) $fdates = array_merge ($fdates, $_SESSION['config']['fdates']);

$dates = "{$_REQUEST['dates']}";
$sdates = ($dates != "" ? "AND (INVENT.DATES  = '$dates')" : "");	

$dfirm = array (100=>'', 215=>L118, 212=>N036, 202=>G022, 203=>D025, 214=>Z004, 204=>K028, 205=>K027, 206=>K061, 207=>N033, 208=>N006, 209=>S039, 210=>U046, 201=>N081);
$firm = ("{$_REQUEST['firm']}");

$fmx[] .= '';
if ($_SESSION['config']['fmx']) $fmx = array_merge ($fmx, $_SESSION['config']['fmx']);
$mx = ("{$_REQUEST['mx']}");
$smx = ($mx!="" ? "AND (INVENT.MX  = '$mx')" : "");	
$tmxarr = Array ("В","Ш","П","Х","C"); 

$fnames[] .= '';
if ($_SESSION['config']['fnames']) $fnames = array_merge ($fnames, $_SESSION['config']['fnames']);
$names = ("{$_REQUEST['names']}");
$snames = ($names!="" ? "AND (SELLERS.SLR_NAME  = '$names')" : "");	

$prodname = ("{$_REQUEST['prodname']}");
$sprodname = ($prodname!="" ? "AND (INVENT.PRODNAME like '$prodname')" : "");

$froles[] .= '';
if ($_SESSION['config']['froles']) $froles = array_merge ($froles, $_SESSION['config']['froles']);
$roles = ("{$_REQUEST['roles']}");
$sroles = ($roles!="" ? "AND (INVENT.ROLES  = '$roles')" : "");	


$valid = $_SESSION['config']['valid'] = ("{$_REQUEST['valid']}") ? "{$_REQUEST['valid']}" : $_SESSION['config']['valid'];

if ($firm) { 
	$database = "192.168.1.".$firm.":C:/Program Files/OnlineServerPro/Database/Database.gdb";
	$username = "SYSDBA";
	$password = "masterkey";
	$charset = "win1251";
	$db = ibase_connect($database, $username, $password, $charset);
	if ($db) {$stview = "
		SELECT DISTINCT
			INVENT.ID as ID, 
			INVENT.DATES as DATES,
			INVENT.TIMES as TIMES,
			CONFIG.CFG_VALUE as FIRM,
			INVENT.MX as MX,
			SELLERS.SLR_NAME as SELLER,
			INVENT.ROLES as ROLES,
			INVENT.BARCODE as BARCODE,
			INVENT.CFC as CFC,
			INVENT.QTY as QTY,
			INVENT.PRODNAME as PRODNAME,
			INVENT.VALID as VALID			
		FROM INVENT,
		SELLERS,
		CONFIG
		WHERE 
		   (		    
			  (CONFIG.CFG_PARAM = 'CODEFIRM')
		   AND 
			  (SELLERS.SLR_CODE = INVENT.SELLER)
           $sdates 
		   $smx
		   $snames
		   $sroles
		   $sprodname		   
		   )";		
		$resultview = ibase_query($stview, $db);
	}
		$n = 1;
		while ($rows = ibase_fetch_assoc($resultview)){
			$result[$n]['N'] = $n;
			$result[$n]['ID'] = $rows['ID'];
			$result[$n]['DATES'] = $fdates[] = $rows['DATES'];
			$result[$n]['TIMES'] = $rows['TIMES'];
			$result[$n]['FIRM'] = $rows['FIRM'];
			$result[$n]['MX'] =  $fmx[] = $rows['MX'];
			$result[$n]['SELLER'] = $fnames[] = $rows['SELLER'];
			$result[$n]['ROLES'] = $froles[] = $rows['ROLES'];
			$result[$n]['BARCODE'] = $rows['BARCODE'];
			$result[$n]['CFC'] = (0+$rows['CFC']);
			$result[$n]['QTY'] = (0+$rows['QTY']);
			$result[$n]['PRODNAME'] = $rows['PRODNAME'];
			$result[$n]['VALID'] = $rows['VALID'];
			$n++;
		}
	ibase_close($db);
}

$fdates[] .= '';
$fdates[] .= date("Y-m-d");
$fdates = array_unique($fdates);
asort($fdates);
$_SESSION['config']['fdates'] = $fdates;

$fmx[] .= '';
$fmx = array_unique($fmx);
asort($fmx);
$_SESSION['config']['fmx'] = $fmx;

$fnames[] .= '';
$fnames =  array_unique($fnames);
asort($fnames);
$_SESSION['config']['fnames'] = $fnames;

$froles[] .= '';
$froles =  array_unique($froles);
asort($froles);
$_SESSION['config']['froles'] = $froles;

//var_dump($_SESSION); 

?>
<html>
<head>
	<title>Справочная служба - Журнал инвентаризации товара</title>
	<meta http-equiv="Content-Type" content="text/html; charset=windows-1251">
	<link rel="STYLESHEET" type="text/css" href="mainpage.css">
	<script src="sorttable.js" type="text/javascript"></script>
	<style type="text/css">

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

<?php
//Далее выводим итоговую таблицу:
echo "<table class = 'sortable' border='1' cellpadding='5'>";
echo '<form action="invview.php" method="get">';
	echo "<thead>";
		echo "<th style='border:1px solid black;' ><font size='-1'>";
			echo '<input type="submit" value="OK">';
		echo "</font></th>";
		echo '<th style="border:1px solid black;" ><font size="-1">';
			 echo '<select name="firm" size="1">';
				foreach($dfirm as $fkeys => $frm) {
					if ($fkeys == $firm) $selected = 'selected';
					else $selected = '';
					$frm = ($fkeys == '' ? 'Фирма' : $frm);
					echo '<option value="'.$fkeys.'" '.$selected.'> '.$frm.'</option>';
				}
		echo "</font></select></th>";
		echo '<th style="border:1px solid black;" ><font size="-1">';
			echo '<select name="dates" size="1">';
			foreach($fdates as $fd) {
					if ($fd == $dates) $selected = 'selected';
					else $selected = '';
					$fds = ($fd == '' ? 'Дата' : $fd);
					echo '<option value="'.$fd.'" '.$selected.'> '.$fds.'</option>';
				}
		echo "</font></select></th>";
		echo "<th style='border:1px solid black;' ><font size='-1'>Время</font></th>";
		echo '<th style="border:1px solid black;" ><font size="-1">';
			echo '<select name="roles" size="1">';
				foreach($froles as $fr) {
					if ($fr == $roles) $selected = 'selected';
					else $selected = '';
					echo '<option value="'.$fr.'" '.$selected.'> '.$fr.'</option>';
				}
		echo "</font></select></th>";
		echo '<th style="border:1px solid black;" ><font size="-1">';
			echo '<select name="names" size="1">';
				foreach($fnames as $fn) {
					if ($fn == $names) $selected = 'selected';
					else $selected = '';
					echo '<option value="'.$fn.'" '.$selected.'> '.$fn.'</option>';
				}
		echo "</font></select></th>";
		echo '<th style="border:1px solid black;" ><font size="-1">';
			echo '<select name="mx" size="1">';
				foreach($fmx as $mkeys => $mxn) {
					if ($mxn == $mx) $selected = 'selected';
					else $selected = '';
					echo '<option value="'.$mxn.'" '.$selected.'> '.$tmxarr[(substr($mxn, 0, 1))-1].substr($mxn, 1, 2).'</option>';
				}
		echo "</font></select></th>";		
		//echo "<th style='border:1px solid black;' ><font size='-1'>Штрихкод</font></th>";
		echo "<th style='border:1px solid black;' ><font size='-1'>";
			echo '<a href="invview.php"> <img src="f.png" alt="Ошибка" border="0" title="Отменить фильтр"></a>';
		echo "</font></th>";
		echo "<th style='border:1px solid black;' ><font size='-1'>";
		echo '<a href="invent.php?firm='.$firm.'&dates='.$dates.'&filtr=1" title="Развернуть">Факт</a>';	 						
		echo "</font></th>";
		echo "<th style='border:1px solid black;' ><font size='-1'>Наименование_фактически_записанного_операторами_товара_в_ведомость</font></th>";
	echo "</thead></form>";
	echo "<tbody>";
	if ($result){
		foreach ($result as $key => $row) {
			echo "<tr>";
					if('3' == $result[$key]['VALID']) $cellt = '<img src="3.gif" alt="Не считали" border="0" title="Продажи до учёта">';
					if('2' == $result[$key]['VALID']) $cellt = '<img src="0.gif" alt="Ошибка" border="0" title="Проверить">';
					if('1' == $result[$key]['VALID']) $cellt = '<img src="1.gif" alt="Готово" border="0" title="Отменить">';
					if('0' == $result[$key]['VALID']) $cellt = '<img src="2.gif" alt="Проверка" border="0" title="Установить">';
				echo "<td>".$result[$key]['N']."</td>";
				echo "<td>".$result[$key]['FIRM']."</td>";
				echo "<td>".$result[$key]['DATES']."</td>";
				echo "<td>".$result[$key]['TIMES']."</td>";
				echo "<td>".$result[$key]['ROLES']."</td>";				
				echo "<td>".$result[$key]['SELLER']."</td>";
				echo "<td>".$tmxarr[(substr($result[$key]['MX'], 0, 1))-1].substr($result[$key]['MX'], 1, 2)."</td>";
					if ($result[$key]['ID']) echo '<td><a href="inverr.php?firm='.$firm.'&id='.$result[$key]['ID'].'&dates='.$dates.'">'.$cellt."</a></td>";
					else echo "<td>".$cellt."</a></td>";
				//echo "<td>".$result[$key]['BARCODE']."</td>";
				echo '<td><a href="invview.php?firm='.$firm.'&dates='.$dates.'&prodname='.$result[$key]['PRODNAME'].'" title="уп: '.($result[$key]['CFC']).' * кол: '.($result[$key]['QTY']).'">'.($result[$key]['QTY']*$result[$key]['CFC']).'</a></td>';
				echo "<td>".$result[$key]['PRODNAME']."</td>";
			echo "</tr>";
		}
	}		
	echo "</tbody>";
echo "</table>";
echo "</div>";
?>
</body>
</html>