package com.devsuperior.dscatalog.projections;

public interface IdProjection<E> {

    //Eventualmente esse ID pode não ser mais Long,
    //pode ser String, UUID. Portanto, a interface irá usar <E>
    E getId();

}
