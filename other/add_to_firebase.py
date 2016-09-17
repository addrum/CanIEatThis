from firebase import firebase
firebase = firebase.FirebaseApplication('https://canieatthis.firebaseio.com/', None)
key = '/yeast'
location = '/ingredients/'
# new = {
# 	'lactose_free' : True,
# 	'vegetarian' : True,
# 	'vegan' : True,
# 	'gluten_free' : True
# }

# use below for places
# location = '/places'
# new = {
	# '51.55375900000001,-0.291529' : {
	# 'dairy_free' : False,
	# 'vegetarian' : True,
	# 'vegan' : True,
	# 'gluten_free' : True
# }

result = firebase.patch(location + key, new)
print(key)
print(result)