WITH aveage AS (
	 SELECT a.code, round(avg(a.close),0)  AS average from investar.daily_price a  
		WHERE 1=1  
		AND ? < a.date  AND A.DATE < ?
		GROUP BY a.code
)

SELECT a.CODE, com.company , a.date AS a, b.date AS b, a.close as aclose, b.close as bclose, (b.close - a.close) as diff, average, 
	
	case when (average > b.close) then 'up'
		ELSE 'down' END updown,
	com.mainItem FROM 

	( SELECT a.code, a.date, a.close from investar.daily_price a  
		WHERE 1=1 
		AND a.date = ? ) AS  a
	INNER JOIN
	(SELECT b.code, b.date, b.close from investar.daily_price b  
		WHERE 1=1 
		AND b.date = ? ) AS  b
	ON a.code = b.code
	INNER JOIN investar.company_info com
	ON a.code = com.code
	
	INNER JOIN aveage
	ON a.code = aveage.code
	
ORDER BY diff DESC

