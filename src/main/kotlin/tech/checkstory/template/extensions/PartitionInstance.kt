package tech.checkstory.template.extensions

public inline fun <Item, reified TypeToFind> Iterable<Item>.requireElementOfType(): Pair<TypeToFind, ArrayList<Item>> {
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
    check(first != null) {
        "The collection does not contain an element of type ${TypeToFind::class}"
    }
    return Pair(first, second)
}
