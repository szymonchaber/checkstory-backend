package tech.checkstory.template.extensions

inline fun <Item, reified TypeToFind> Iterable<Item>.requireStartsWith(): Pair<TypeToFind, ArrayList<Item>> {
    val pair = getIfStartsWith<Item, TypeToFind>()
    check(pair != null) {
        "The collection does not contain an element of type ${TypeToFind::class}"
    }
    return pair
}

inline fun <Item, reified TypeToFind> Iterable<Item>.getIfStartsWith(): Pair<TypeToFind, ArrayList<Item>>? {
    var first: TypeToFind? = null
    val second = ArrayList<Item>()
    for (element in this) {
        if (element is TypeToFind) {
            check(first == null) {
                "The collection contains more than one element of type ${TypeToFind::class}"
            }
            first = element
        } else {
            second.add(element)
        }
    }
    return first?.let {
        Pair(it, second)
    }
}
